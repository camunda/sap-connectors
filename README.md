# Camunda SAP Connectors – Central Repository

> [!WARNING]
> **`release/8.6` is DEPRECATED.** The 8.6 line of the Camunda SAP connectors is no longer actively maintained:
> no new features, no routine dependency updates and no regular patch releases.
>
> - Use a supported version instead. The supported release lines are listed on the [`main` branch](https://github.com/camunda/sap-connectors/tree/main).
> - This branch is kept open **for emergencies only** (e.g. a critical security fix that must be shipped for 8.6).
>   Such changes are still accepted and released as usual, see [RELEASE.md](./RELEASE.md).

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

