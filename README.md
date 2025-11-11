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

## Release Setup

We follow the standard connectors pattern of having 
release branches aligned to the Camunda product releases, 
e.g. `release/8.8.x` for Camunda 8.8.x releases.

Unlike the Camunda main repo, we only develop for released versions
of the product so we don't have an `alpha/` branch.

To trigger a release invoke [the release workflow](https://github.com/camunda/sap-connectors/actions/workflows/build-and-publish.yml)
manually. You will be asked for the target branch and the patch version.
The workflow will them publish the docker image to docker hub and 
publish the WAR of the RFC connector to the GitHub releases of the repo.

## Development Hints

Please consider using asdf and direnv to manage your local development environment.
Direnv will use the Camunda Vault to access the credentials required for
running the unit tests. While asdf will ensure you have the right versions of
all required tools.
Also consider running `pre-commit install` to run a subset of our CI checks on every commit.
Additionally, you'll have to build the docker image for the CAP bookshop backend 
from [odata-connector/cap-bookshop](odata-connector/cap-bookshop) 
before running the unit tests of the OData connector.

# Regarding the release/8.6 branch

The 8.6 release of the SAP connectors got broken during migration
as we decided to test against our INT environment but still wanted 
to support the Console Secret provider in the connector runtime.

This secret provider requires the Camunda/Zeebe Client mode to be SaaS,
but the 8.5 client, which we use in the connectors 8.6 release doesn't support
the `withDomain` method, so we can redirect the SaaS client to INT.

This leaves us with the following options:
- We ship a release without end-to-end testing, 
which is very useful to catch issues in the mtad.yaml.example
- We remove the check for client mode SaaS in the connectors runtime
- We upgrade the connectors runtime to use spring-zeebe/spring-camunda for 8.6
- We don't allow customers to use the Console Secret provider when deploying with the csap utility



