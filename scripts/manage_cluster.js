#!/usr/bin/env node
/**
 * manage_cluster.js (CI version - channel & generation selection + provisioning)
 *
 * Steps:
 *   1. getOAuthToken
 *   2. selectChannelAndGeneration (includes parameter fetch + generation selection)
 *   3. createCluster (includes readiness polling)
 *   4. createClient (includes connection detail retries)
 *   5. printSummary (final JSON + exports already set)
 */

const fs = require('fs');

// ---------------- Constants / Config ----------------
const BASE_HOSTNAME = process.env.BASE_HOSTNAME || 'ultrawombat.com';
const AUDIENCE = `api.cloud.${BASE_HOSTNAME}`;
const LOGIN_HOST = `login.cloud.${BASE_HOSTNAME}`;
const API_BASE_URL = `https://api.cloud.${BASE_HOSTNAME}`;
const CLUSTER_NAME = process.env.CLUSTER_NAME || 'SAP-CONNECTORS-E2E-CLUSTER';
const POLL_INTERVAL = parseInt(process.env.POLL_INTERVAL || '20');
const POLL_TIMEOUT = parseInt(process.env.POLL_TIMEOUT || '900'); // 15 minutes

// ---------------- Logging helpers ----------------
function log(...args) {
    console.log('[INFO]', ...args);
}

function ok(...args) {
    console.log('[OK]', ...args);
}

function warn(...args) {
    console.log('[WARN]', ...args);
}

function fail(...args) {
    console.error('[ERROR]', ...args);
    process.exit(1);
}

// Derived globals
const TOKEN_ISSUER = `https://${LOGIN_HOST}/oauth/token`;

// Write GitHub Actions outputs if GITHUB_OUTPUT is set
function writeGithubOutput(clientId, clientSecret, zeebeGrpcAddress, zeebeRestAddress, clusterId) {
    const githubOutput = process.env.GITHUB_OUTPUT;
    if (!githubOutput) return;

    try {
        if (!fs.existsSync(githubOutput) || !fs.statSync(githubOutput).isFile()) {
            warn(`GITHUB_OUTPUT path not writable: ${githubOutput}`);
            return;
        }

        const outputs = [
            `client-id=${clientId}`,
            `client-secret=${clientSecret}`,
            `grpc-address=${zeebeGrpcAddress}`,
            `rest-address=${zeebeRestAddress}`,
            `cluster-uuid=${clusterId}`
        ].join('\n') + '\n';

        fs.appendFileSync(githubOutput, outputs);
        // Prevents secret from being logged in GitHub Actions
        console.log("::add-mask::" + clientSecret);
        ok('Wrote GitHub outputs (client-id, client-secret, grpc-address, rest-address)');
    } catch (error) {
        warn(`Failed to write GitHub outputs: ${error.message}`);
    }
}

// ---------------- Step 1: OAuth Token ----------------
async function getOAuthToken() {
    log(`BASE_HOSTNAME=${BASE_HOSTNAME}`);
    log(`Authenticating against https://${LOGIN_HOST} (audience=${AUDIENCE})`);

    try {
        const response = await fetch(TOKEN_ISSUER, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                audience: AUDIENCE,
                grant_type: 'client_credentials',
                client_id: process.env.CONSOLE_CLIENT_ID,
                client_secret: process.env.CONSOLE_CLIENT_SECRET
            })
        });

        if (!response.ok) {
            const errorText = await response.text();
            fail(`Failed to obtain access token (${response.status}): ${errorText}`);
        }

        const data = await response.json();
        if (!data.access_token) {
            fail(`Failed to obtain access token: No access_token in response`);
        }

        const accessToken = data.access_token;
        ok('Obtained access token');
        return accessToken;
    } catch (error) {
        fail(`Failed to obtain access token: ${error.message}`);
    }
}

// Step 2: Select Channel and Generation
async function selectChannelAndGeneration(accessToken) {
    log(`Fetching cluster parameters from ${API_BASE_URL}/clusters/parameters`);

    try {
        const response = await fetch(`${API_BASE_URL}/clusters/parameters`, {
            headers: {
                'Authorization': `Bearer ${accessToken}`,
                'Accept': 'application/json'
            }
        });

        if (!response.ok) {
            const errorText = await response.text();
            fail(`Failed to fetch cluster parameters (${response.status}): ${errorText}`);
        }

        const parametersJson = await response.json();
        if (!parametersJson.channels) {
            fail('Unexpected parameters response (no .channels)');
        }

        // Find Stable channel
        const channelJson = parametersJson.channels.find(channel => channel.name === 'Stable');
        if (!channelJson) {
            fail('Stable channel not present');
        }

        log(`Found Stable channel with UUID: ${channelJson.uuid}`);

        const desiredGeneration = process.env.CAMUNDA_DESIRED_GENERATION;

        // Find exact matches for the prefix and generation number
        const candidatesJson = channelJson.allowedGenerations.filter(gen => {
            const extractedGen = gen.name.replace(/Camunda /g, '').replace(/\+gen\d+$/, '');
            if (!extractedGen) return false;
            return extractedGen === desiredGeneration;
        });

        if (candidatesJson.length === 0) {
            fail(`No generations in Stable match '${desiredGeneration}'`);
        }

        if (candidatesJson.length > 1) {
            fail(`Multiple generations found matching '${desiredGeneration}': ${candidatesJson.map(g => g.name).join(', ')}`);
        }

        // Select the single matching generation
        const selected = candidatesJson[0];
        // Select plan and region
        if (!parametersJson.clusterPlanTypes || parametersJson.clusterPlanTypes.length === 0) {
            fail('No cluster plan types available');
        }

        const planJson = parametersJson.clusterPlanTypes[0];
        if (!parametersJson.regions || parametersJson.regions.length === 0) {
            fail('No regions available');
        }
        const regionJson = parametersJson.regions[0];

        return {
            channelName: channelJson.name,
            channelUuid: channelJson.uuid,
            genName: selected.name,
            genUuid : selected.uuid,
            candidatesJson,
            planUuid: planJson.uuid,
            planName: planJson.name,
            regionUuid: regionJson.uuid,
            regionName: regionJson.name
        };

    } catch (error) {
        fail(`Failed to select channel and generation: ${error.message}`);
    }
}

// Step 3: Create Cluster
async function createCluster(accessToken, selection) {
    const { channelName, channelUuid, genName, genUuid, planUuid, planName, regionName, regionUuid } = selection;

    log(`Creating cluster name='${CLUSTER_NAME}' plan='${planName}' region='${regionName}' channel='${channelName}' generation='${genName}'`);

    try {
        const response = await fetch(`${API_BASE_URL}/clusters`, {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${accessToken}`,
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                name: CLUSTER_NAME,
                planTypeId: planUuid,
                channelId: channelUuid,
                generationId: genUuid,
                regionId: regionUuid
            })
        });

        if (!response.ok) {
            const errorText = await response.text();
            fail(`Cluster creation failed (${response.status}): ${errorText}`);
        }

        const data = await response.json();
        if (!data.clusterId) {
            fail(`Cluster creation failed: No clusterId in response`);
        }

        const clusterId = data.clusterId;
        ok(`Cluster created id=${clusterId} (pending readiness)`);

        // Poll for readiness
        const startTime = Date.now();
        let clusterJson;
        while (true) {
            const clusterResponse = await fetch(`${API_BASE_URL}/clusters/${clusterId}`, {
                headers: {
                    'Authorization': `Bearer ${accessToken}`,
                    'Accept': 'application/json'
                }
            });

            if (!clusterResponse.ok) {
                const errorText = await clusterResponse.text();
                fail(`Failed to fetch cluster status (${clusterResponse.status}): ${errorText}`);
            }

            clusterJson = await clusterResponse.json();
            if (!clusterJson.status) {
                fail(`Malformed cluster status response: No status field`);
            }

            const readyState = clusterJson.status.ready;
            if (!readyState) {
                fail(`Malformed cluster status response: No ready field in status`);
            }

            log(`Cluster state: ${readyState}`);

            if (readyState === 'Healthy') {
                ok('Cluster is Healthy');
                break;
            }

            if (readyState === 'Unhealthy') {
                fail('Cluster became Unhealthy');
            }

            const now = Date.now();
            if (now - startTime > POLL_TIMEOUT * 1000) {
                fail(`Timeout waiting for cluster to become Healthy (last state=${readyState})`);
            }

            await new Promise(resolve => setTimeout(resolve, POLL_INTERVAL * 1000));
        }

        const finalPlanName = clusterJson.planType.name;
        const finalRegionName = clusterJson.region.name;
        const zeebeAddress = clusterJson.links.zeebe;
        const operateAddressRaw = clusterJson.links.operate;

        return {
            clusterId,
            clusterJson,
            finalPlanName,
            finalRegionName,
            zeebeAddress,
            operateAddressRaw
        };

    } catch (error) {
        fail(`Failed to create cluster: ${error.message}`);
    }
}

// Compute final Zeebe REST + gRPC addresses (post processing)
function processAddresses(zeebeAddress, operateAddressRaw) {
    if (!zeebeAddress) {
        fail("Missing 'zeebe' address in cluster links (zeebeAddress empty)");
    }
    if (!operateAddressRaw) {
        fail("Missing 'operate' address in cluster links (operateAddressRaw empty)");
    }

    const zeebeGrpcAddress = `grpcs://${zeebeAddress}:443`;
    const zeebeRestAddress = operateAddressRaw.replace(/operate/g, 'zeebe');
    ok(`Processed addresses: grpc=${zeebeGrpcAddress} rest=${zeebeRestAddress}`);

    return { zeebeGrpcAddress, zeebeRestAddress };
}

// Step 4: Create Client
async function createClient(accessToken, clusterId) {
    try {
        const response = await fetch(`${API_BASE_URL}/clusters/${clusterId}/clients`, {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${accessToken}`,
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                clientName: `client-${CLUSTER_NAME}`
            })
        });

        if (!response.ok) {
            const errorText = await response.text();
            fail(`Client creation failed (${response.status}): ${errorText}`);
        }

        const data = await response.json();
        if (!data.clientId || !data.clientSecret) {
            fail(`Client creation failed: Missing clientId or clientSecret in response`);
        }

        const clientId = data.clientId;
        const clientSecret = data.clientSecret;
        ok(`Client created id=${clientId}`);

        return { clientId, clientSecret };

    } catch (error) {
        fail(`Failed to create client: ${error.message}`);
    }
}

// Delete cluster by ID
async function deleteCluster(accessToken, clusterId) {
    log(`Deleting cluster id=${clusterId}`);

    try {
        const response = await fetch(`${API_BASE_URL}/clusters/${clusterId}`, {
            method: 'DELETE',
            headers: {
                'Authorization': `Bearer ${accessToken}`
            }
        });

        if (!response.ok) {
            const errorText = await response.text();
            fail(`Cluster deletion failed (${response.status}): ${errorText}`);
        }

        ok(`Cluster deletion request sent for id=${clusterId}`);
    } catch (error) {
        fail(`Failed to delete cluster: ${error.message}`);
    }
}

// Step 5: Print Summary
function printSummary(accessToken, selection, clusterInfo, clientInfo, addresses) {
    const { channelName, channelUuid, genName, genUuid, candidatesJson } = selection;
    const { clusterId, finalPlanName} = clusterInfo;
    const { clientId, clientSecret } = clientInfo;
    const { zeebeGrpcAddress, zeebeRestAddress } = addresses;
    // This marks the clientSecret as a secret in GitHub Actions logs
    writeGithubOutput(clientId, clientSecret, zeebeGrpcAddress, zeebeRestAddress, clusterId);

    // Build final JSON
    const finalJson = {
        status: 'success',
        desiredPrefix: process.env.CAMUNDA_DESIRED_GENERATION,
        channel: {
            name: channelName,
            uuid: channelUuid
        },
        generation: {
            name: genName,
            uuid: genUuid
        },
        cluster: {
            id: clusterId,
            name: CLUSTER_NAME,
            plan: finalPlanName,
            region: selection.regionName,
            restAddress: zeebeRestAddress,
            grpcAddress: zeebeGrpcAddress
        },
        client: {
            id: clientId,
            secret: clientSecret,
            oauth: TOKEN_ISSUER
        },
        candidateGenerations: candidatesJson
    };

    console.log(JSON.stringify(finalJson, null, 2));
    ok('Cluster and client provisioning complete');
    log(`Artifacts exported: cluster id=${clusterId}, client id=${clientId}`);
}

function dumpClientCredentials(outputPath, clientInfo, addresses) {
    if(fs.existsSync(outputPath)) {
        warn(`File at ${outputPath} already exists. Overwriting.`);
    }
    fs.writeFileSync(outputPath, JSON.stringify({
        clientId: clientInfo.clientId,
        clientSecret: clientInfo.clientSecret,
        grpcAddress: addresses.zeebeGrpcAddress,
        restAddress: addresses.zeebeRestAddress}));
    ok(`Wrote client credentials to ${outputPath}`);
}

// Main function
async function main() {
    const args = process.argv.slice(2);

    function requireEnv(varName) {
        if (!process.env[varName]) {
            fail(`${varName} is required`);
        }
    }
    requireEnv('CONSOLE_CLIENT_ID');
    requireEnv('CONSOLE_CLIENT_SECRET');

    const accessToken = await getOAuthToken();

    // Check for deletion flag
    if (args[0] === '-d') {
        if (!args[1]) {
            fail('Cluster ID required when using -d flag. Usage: node manage_cluster.js -d <cluster_id>');
        }
        await deleteCluster(accessToken, args[1]);
        ok('Cluster deletion completed');
        return;
    }

    requireEnv('CAMUNDA_DESIRED_GENERATION');
    const selection = await selectChannelAndGeneration(accessToken);
    const clusterInfo = await createCluster(accessToken, selection);
    const addresses = processAddresses(clusterInfo.zeebeAddress, clusterInfo.operateAddressRaw);
    const clientInfo = await createClient(accessToken, clusterInfo.clusterId);
    if(args[0]) {
        const outputPath = args[0];
        dumpClientCredentials(outputPath, clientInfo, addresses);
    }

    printSummary(accessToken, selection, clusterInfo, clientInfo, addresses);
}

// Run main function
if (require.main === module) {
    main().catch(error => {
        fail(`Unhandled error: ${error.message}`);
    });
}
