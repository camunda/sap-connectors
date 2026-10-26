# Camunda-SAP integration: SAP OData protocol outbound Connector

Camunda Connector to interact with an SAP S/4 and ECC system via OData v2 + v4.
It is distributed as [a Docker image](https://hub.docker.com/repository/docker/camunda/sap-odata-connector) and needs deployment to BTP.

### Maintenance compatibility

The supported worker remains `io.camunda:sap-odata:2`, with simple and batch
requests under `requestDetails`. Flat v1 requests and historical worker types
are not supported by this patch; migrate them to the current template.
Missing or null `requestDetails` fails input validation before execution.
Malformed shapes or discriminators fail binding; not every nested field is validated.
Batch response schemas are unchanged: V2 retains `result.d`, including
`d.results` and `d.__next`; successful empty 204 responses remain successful.

### Docker startup regression

The 8.8 Dockerfile sets `SPRING_HTTP_CONVERTERS_PREFERRED_JSON_MAPPER=jackson`.
The `8.8.24` base image still supplies `jackson2`, which its Spring Boot 3 enum does not
support. Keep this correction specific to 8.8; do not copy it to Boot 4 runtimes.

Runtime `8.8.24` also fixes Guava dependency mediation. In `8.8.22`, the launcher
loads Guava 33.5.0 from `/opt/app` before the fixed 33.7.2 in the SAP custom JAR;
a custom-JAR version pin alone does not protect the running application.

After packaging and building a local image, run with a Java 21+ JDK:

```bash
bash scripts/test-odata-runtime.sh sap-odata-local
```

CI runs the same check on its test image before publication. The publishing job
rebuilds the final-version artifact separately; this is not artifact promotion.
The check uses a temporary test-only Java agent in the image's actual JVM to
verify the loaded origins and fixed 33.7.2 bytes of the three affected Guava
collection classes. It fails on the old shadowing runtime; update the expected
hashes only after verifying a future Guava upgrade. The agent is not packaged
in the product image.
The check requires completed Spring startup
and actuator liveness `UP`, with loopback-only client addresses and no SAP
credentials. Workers are registered; inbound polling and webhooks are disabled.
This is not a broker-readiness or SAP connectivity test. With a matching
local Camunda stack, separately verify `/actuator/health` is `UP` and execute a
bookshop process.

Draft PRs skip RFC deployment, live SAP E2E, and undeployment jobs. Local tests
and the normal build workflow still run; a skipped SAP E2E job is not a pass.

## development hints

- Use asdf and direnv to manage your dependencies and get all env vars set up
- C8.7, either locally or SaaS
- source code formatting is done with `maven-spotless-plugin` upon build/compile

- on PRs
  - don't bump the project version in regular feature/fix PRs. The maintenance branch already carries the next patch as `-SNAPSHOT`; after each release it is advanced in a dedicated version PR.
  - don't change major or minor, as they indicate the Camunda 8 release association

### HeroDevs Never-Ending Support (NES) dependencies

`spring-boot-starter-camunda-connectors` (test scope) transitively pulls Camunda-patched,
pre-release Spring artifacts (e.g. `6.2.19-spring-framework-6.2.21`) that only exist on
Camunda's internal Artifactory (`herodevs-nes` repository in `pom.xml`). The public HeroDevs
endpoint returns `401` without authentication, so a clean local `mvn install`/`mvn test` needs
a mirror + credentials configured in `~/.m2/settings.xml`, same as CI:

```xml
<mirror>
  <id>camunda-nexus</id>
  <mirrorOf>camunda-nexus,herodevs-nes</mirrorOf>
  <url>https://repository.nexus.camunda.cloud/content/groups/internal/</url>
</mirror>
```

with a matching `<server>` entry (`id: camunda-nexus`) supplying your internal Artifactory
credentials. Without this, local builds fail with `401` once the local Maven cache no longer
has these artifacts. Ask a team member for credentials if you don't have internal Artifactory access.

### OData sample backend

There's a Node.js-based OData v2 + v4 backend located in `/cap-bookshop`.
It is intended for dev-time and mandatory for running the unit tests.

The unit tests expect a locally built Docker image for the CAP bookshop backend
so build the image (unpublished, unconventional tag on purpose) before running the tests:

```bash
docker build -t camunda/sap-odata-connector/cap-bookshop odata-connector/cap-bookshop
```

## Release cutting

&rarr; will always
- publish a docker image
- do a GH release

### rolling 8.x release
:warning: GH releases is only done upon changes to `pom.xml` in a push to a `release/8.x` branch.
- adjust version in `/src/pom.xml` (minor version)
- generate the connector template w/ the respective maven task
- push changes to `release/8.x` branch

### new 8.x release
- create release branch: `release/8.x`
- adjust version in `/src/pom.xml`
- in `.github/workflows/build-and-publish.yml`:
    - adjust `on.push.branches` to the release branch
    - adjust `CAMUNDA_CONNECTORS_VERSION`
- in `.github/workflows/build-and-test.yml`:
    - adjust `on.pull_request.branches` to the release branch
- adjust secrets in both GH secrets to point to the target 8.x cluster
- adjust `secrets.C8x_...` in `.github/**/*.yml` to point the target cluster version
