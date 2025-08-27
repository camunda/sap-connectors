# Camunda SAP Connectors – Central Repository

This repository serves as the central location for all Camunda SAP connectors, providing integration solutions between Camunda and SAP S/4/ECC systems. It aggregates multiple connector projects, each tailored to a specific SAP protocol and deployment scenario.

## Overview of Connectors

### SAP RFC Connector
- **Protocol:** SAP RFC (Remote Function Calls) to BAPIs and remote-enabled Function Modules
- **Deployment:** Java `.war` application for Cloud Foundry
- **Special Requirement:** Requires SAP Java Connector (JCo) installed on the target system (not distributable via Docker due to SAP licensing)
- **Documentation:** See [`rfc-connector/README.md`](./rfc-connector/README.md)

### SAP OData Connector
- **Protocol:** SAP OData v2 and v4
- **Deployment:** Distributed as a Docker image for SAP BTP
- **Documentation:** See [`odata-connector/README.md`](./odata-connector/README.md)

## Development & Release
Each connector has its own development and release workflow. Please refer to the respective connector's README for instructions on local development, testing, and release management:
- [RFC Connector Development & Release](./rfc-connector/README.md)
- [OData Connector Development & Release](./odata-connector/README.md)

