# Camunda-SAP integration: SAP OData protocol outbound Connector

Camunda Connector to interact with an SAP S/4 and ECC system via OData v2 + v4.
It is distributed as [a Docker image](https://hub.docker.com/repository/docker/camunda/sap-odata-connector) and needs deployment to BTP.


### OData sample backend

There's a Node.js-based OData v2 + v4 backend located in `/cap-bookshop`.
It is intended for dev-time and mandatory for running the unit tests.

The unit tests expect a locally built Docker image for the CAP bookshop backend
so build the image (unpublished, unconventional tag on purpose) before running the tests:

```bash
docker build -t camunda/sap-odata-connector/cap-bookshop odata-connector/cap-bookshop
```
