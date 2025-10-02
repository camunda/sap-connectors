# Camunda-SAP integration: SAP OData protocol outbound Connector

Camunda Connector to interact with an SAP S/4 and ECC system via OData v2 + v4.
It is distributed as [a Docker image](https://hub.docker.com/repository/docker/camunda/sap-odata-connector) and needs deployment to BTP.

## development hints

- Use asdf and direnv to manage your dependencies and get all env vars set up
- C8.7, either locally or SaaS
- source code formatting is done with `maven-spotless-plugin` upon build/compile

- on PRs
  - always bump the patch version first in `pom.xml`
  - don't change major or minor, as they indicate the Camunda 8 release association

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
