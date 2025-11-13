# Agent Information for Camunda SAP Connectors

This file contains information for AI agents working on the Camunda SAP Connectors repository.

## Project Overview

This repository serves as the central location for all Camunda SAP connectors, providing integration solutions between Camunda and SAP S/4/ECC systems. It contains multiple connector projects, each tailored to a specific SAP protocol and deployment scenario.

### Connectors

1. **SAP RFC Connector** (`rfc-connector/`)
   - Protocol: SAP RFC (Remote Function Calls) to BAPIs and remote-enabled Function Modules
   - Deployment: Java `.war` application for Cloud Foundry
   - Special requirement: Requires SAP Java Connector (JCo) installed on target system
   - Not distributable via Docker due to SAP licensing restrictions

2. **SAP OData Connector** (`odata-connector/`)
   - Protocol: SAP OData v2 and v4
   - Deployment: Docker image for SAP BTP
   - Available on Docker Hub: `camunda/sap-odata-connector`

3. **Eventing Connectors** (`eventing-connectors/`)
   - Event-driven integration connectors

## Essential Commands

### Build & Test

```bash
# Build all modules
mvn clean install

# Build specific connector
mvn clean install -pl rfc-connector
mvn clean install -pl odata-connector

# Run code formatting
mvn spotless:apply

# Check code formatting
mvn spotless:check

# Build OData CAP bookshop backend (required for OData tests)
docker build -t camunda/sap-odata-connector/cap-bookshop odata-connector/cap-bookshop
```

### Code Quality

```bash
# Check code formatting (via pre-commit)
pre-commit run --all-files

# Format code with Spotless
mvn spotless:apply
```

### Development Setup

- Use `asdf` and `direnv` to manage dependencies and environment variables
- Requires Java 25 (see `.tool-versions`)
- Pre-commit hooks are configured in `.pre-commit-config.yaml`

## Project Structure

```
.
├── rfc-connector/          # SAP RFC connector (.war for Cloud Foundry)
├── odata-connector/        # SAP OData connector (Docker image)
│   └── cap-bookshop/      # OData v2/v4 test backend (Node.js)
├── eventing-connectors/    # Event-driven connectors
├── blueprint/             # BPMN blueprint files and forms
├── scripts/               # Build and deployment scripts
├── pom.xml               # Parent Maven POM
├── .pre-commit-config.yaml # Pre-commit hooks configuration
└── renovate.json5        # Renovate bot configuration
```

## Development Workflow

### RFC Connector Development

1. **Setup JCo** (optional for local development):
   ```bash
   mvn install:install-file -Dfile=sapjco3.jar \
     -DgroupId=com.sap.conn.jco \
     -DartifactId=com.sap.conn.jco.sapjco3 \
     -Dversion=3.1.10 \
     -Dpackaging=jar
   ```

2. **Configuration**:
   - Create `<dest>.jcoDestination` file in classpath
   - Set environment variable: `destinations=[{"name": "<dest>", "type": "RFC"}]`
   - Uncomment `sapjco` section in `pom.xml` for local development
   - Configure IDE to add dependencies with 'provided' scope to classpath

3. **Development Notes**:
   - Code formatting is automatic via `maven-spotless-plugin` during build/compile
   - Always bump patch version first in PRs
   - Don't change major/minor versions (they indicate Camunda 8 release association)
   - Example: `sap-rfc-connector-8.5.2` → version for Camunda 8.5, connector version 2

### OData Connector Development

1. **Setup**:
   - Use asdf and direnv for dependency management
   - Requires Camunda 8.8 (locally or SaaS) - version is defined in parent `pom.xml` (`version.camunda`)
   - Build CAP bookshop Docker image before running tests:
     ```bash
     docker build -t camunda/sap-odata-connector/cap-bookshop odata-connector/cap-bookshop
     ```

2. **Development Notes**:
   - Code formatting via `maven-spotless-plugin` during build/compile
   - Always bump patch version first in PRs
   - Don't change major/minor versions

## Code Style Guidelines

- Java code is formatted using Google Java Format (via Spotless)
- Indentation: 2 spaces
- UTF-8 encoding for all files
- Trailing whitespaces are removed
- Files must end with newline
- Spotless plugin runs automatically on build/compile
- Pre-commit hooks enforce formatting and linting

## Key Technologies & Dependencies

For current versions, refer to:
- `.tool-versions` - Tool versions managed by asdf (Java, Maven, pre-commit, etc.)
- `pom.xml` - Maven dependencies and versions

Key dependencies include:
- **Build Tool**: Maven
- **Camunda Version**: Defined in parent `pom.xml` as `version.camunda`
- **Testing**: JUnit Jupiter, Mockito, AssertJ, Testcontainers
- **Code Quality**: Spotless Maven Plugin

## GitHub Workflows

### Main Release Workflow
- `build-and-publish.yml` - Orchestrates releases for all connectors via workflow_dispatch

### RFC Connector Workflows
- `rfc-build-and-publish.yml` - RFC connector specific build/publish (called by main workflow)
- `rfc-e2e-test.yml` - RFC end-to-end tests
- `rfc-reusable-deploy.yml` - Reusable RFC deployment workflow
- `rfc-reusable-e2e-test.yml` - Reusable RFC E2E test workflow
- `rfc-reusable-unit-test.yml` - Reusable RFC unit test workflow
- `rfc-reusable-undeploy.yml` - Reusable RFC undeployment workflow

### OData Connector Workflows
- `odata-build-and-publish.yml` - OData connector build/publish (called by main workflow)
- `odata-reusable-build-and-test.yml` - Reusable OData build/test
- `odata-reusable-deploy.yml` - Reusable OData deployment

### General Workflows
- `build-and-test.yml` - Build and test on PRs
- `publish-snapshots.yml` - Publish snapshot versions
- `pre-commit.yml` - Pre-commit hooks validation
- `backport-pr.yml` - Automated backporting

## Important Files

- `pom.xml` - Parent Maven POM with shared configuration
- `renovate.json5` - Dependency update automation
- `.pre-commit-config.yaml` - Pre-commit hooks for code quality
- `.tool-versions` - Tool versions managed by asdf
- `.envrc` - Environment setup for direnv
- `rfc-connector/README.md` - RFC connector specific documentation
- `odata-connector/README.md` - OData connector specific documentation
- `blueprint/` - BPMN process blueprints and Fiori forms

## Common Issues & Solutions

### OData Connector Testing
- **Issue**: Unit tests fail
- **Solution**: Build CAP bookshop Docker image first:
  ```bash
  docker build -t camunda/sap-odata-connector/cap-bookshop odata-connector/cap-bookshop
  ```

### RFC Connector Development
- **Issue**: JCo classes not found
- **Solution**: 
  1. Install JCo to local Maven repo (see RFC Connector Development section)
  2. Uncomment `sapjco` section in `pom.xml`
  3. Configure IDE to include 'provided' scope dependencies

### Code Formatting Issues
- **Issue**: Pre-commit hook fails on formatting
- **Solution**: Run `mvn spotless:apply` to auto-format code

### Version Management
- **Issue**: Not sure which version to use
- **Solution**: 
  - Major.Minor versions match Camunda 8 release (e.g., 8.8.x for Camunda 8.8)
  - Patch version is incremented for connector updates
  - Always bump patch version in PRs

## Testing Guidelines

### RFC Connector
- Unit tests run with standard `mvn test`
- E2E tests run via GitHub Actions workflows
- E2E tests require deployment to Cloud Foundry

### OData Connector
- Unit tests require CAP bookshop Docker image
- Tests use Testcontainers for integration testing
- Build test backend before running tests

## References

For more detailed information, see:
- `README.md` - Repository overview and quick start
- `rfc-connector/README.md` - RFC connector development guide
- `odata-connector/README.md` - OData connector development guide
