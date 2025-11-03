#!/usr/bin/env node

/**
 * Safe replacement script for mtad.yaml placeholders.
 * This script replaces sed to avoid issues with special characters in replacement values.
 */

const fs = require('fs');

function safeReplace(content, placeholder, replacement) {
    /**
     * Safely replace a placeholder with a replacement value.
     * Uses literal string replacement to avoid issues with special regex characters.
     */
    return content.split(placeholder).join(replacement);
}

function main() {
    const args = process.argv.slice(2);
    
    if (args.length !== 6) {
        console.error('Usage: node replace_mtad_placeholders.js <input_file> <app_version> <grpc_address> <rest_address> <client_id> <client_secret>');
        process.exit(1);
    }
    
    const [inputFile, appVersion, grpcAddress, restAddress, clientId, clientSecret] = args;
    const mangledVersion = appVersion.replace(/\./g, '_');

    try {
        // Read the input file
        let content = fs.readFileSync(inputFile, 'utf8');
        
        // Perform safe replacements
        content = safeReplace(content, '<app-version>', appVersion);
        content = safeReplace(content, '<mangled-version>', mangledVersion);
        content = safeReplace(content, '<grpc-address>', grpcAddress);
        content = safeReplace(content, '<rest-address>', restAddress);
        content = safeReplace(content, '<client-id>', clientId);
        content = safeReplace(content, '<client-secret>', clientSecret);
        content = safeReplace(content, '<token-url>', 'https://login.cloud.ultrawombat.com/oauth/token');
        content = safeReplace(content, '<zeebe-audience>', 'zeebe.ultrawombat.com');

        // Handle the connector name replacement (using regex for end-of-line matching)
        const namePattern = /name: sap-rfc-connector$/gm;
        content = content.replace(namePattern, `name: sap-rfc-connector-${appVersion}`);
        
        // Write the modified content back
        fs.writeFileSync(inputFile, content, 'utf8');
        
        console.log(`Successfully replaced placeholders in ${inputFile}`);

    } catch (error) {
        console.error(`Error processing file: ${error.message}`);
        process.exit(1);
    }
}

if (require.main === module) {
    main();
}
