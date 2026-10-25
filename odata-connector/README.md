# Camunda-SAP integration: SAP OData protocol outbound Connector

Camunda Connector to interact with an SAP S/4 and ECC system via OData v2 + v4.
It is distributed as [a Docker image](https://hub.docker.com/repository/docker/camunda/sap-odata-connector) and needs deployment to BTP.

### Maintenance compatibility

The supported worker remains `io.camunda:sap-odata:2`, with simple and batch
requests under `requestDetails`. Flat v1 requests and historical worker types
are not supported by this patch; migrate them to the current template.
Missing, null or malformed request details fail validation before execution.
Batch response schemas are unchanged: V2 retains `result.d`, including
`d.results` and `d.__next`; successful empty 204 responses remain successful.

### Docker startup regression

After packaging and building a local image, run:

```bash
bash scripts/test-odata-runtime.sh sap-odata-local
```

CI runs the same check on its test image before publication. The publishing job
rebuilds the final-version artifact separately; this is not artifact promotion.
The check requires completed Spring startup
and actuator liveness `UP`, with loopback-only client addresses and no SAP
credentials. Workers are registered; inbound polling and webhooks are disabled.
This is not a broker-readiness or SAP connectivity test. With a matching
local Camunda stack, separately verify `/actuator/health` is `UP` and execute a
bookshop process.

Draft PRs skip RFC deployment, live SAP E2E, and undeployment jobs. Local tests
and the normal build workflow still run; a skipped SAP E2E job is not a pass.

### OData sample backend

There's a Node.js-based OData v2 + v4 backend located in `/cap-bookshop`.
It is intended for dev-time and mandatory for running the unit tests.

The unit tests expect a locally built Docker image for the CAP bookshop backend
so build the image (unpublished, unconventional tag on purpose) before running the tests:

```bash
docker build -t camunda/sap-odata-connector/cap-bookshop odata-connector/cap-bookshop
```
