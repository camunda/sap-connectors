# Camunda-SAP integration: SAP OData protocol outbound Connector

Camunda Connector to interact with an SAP S/4 and ECC system via OData v2 + v4.
It is distributed as [a Docker image](https://hub.docker.com/repository/docker/camunda/sap-odata-connector) and needs deployment to BTP.

### Element template compatibility

The current worker (`io.camunda:sap-odata:2`) accepts both v2 requests under
`requestDetails` and the flat v1 fields (`entityOrEntitySet`, `httpMethod`, `payload`).
The legacy worker type `io.camunda:sap:odata:outbound:` is also registered. Both use
the same execution path; the v2 template ID and batch request format are unchanged.
Malformed v2 request details are rejected, not retried as v1 requests.
OData v2 batch responses unwrap the protocol's `d` envelope, matching simple v2
requests; OData v4 response handling is unchanged.

### Docker startup regression

After packaging and building a local image, run:

```bash
bash scripts/test-odata-runtime.sh sap-odata-local
```

CI runs the same check before publication. It requires completed Spring startup
and actuator liveness `UP`, with loopback-only client addresses and no SAP
credentials. Workers are registered; inbound polling and webhooks are disabled.
This is not a broker-readiness or SAP connectivity test. With a matching
local Camunda stack, separately verify `/actuator/health` is `UP` and execute a
bookshop process.


### OData sample backend

There's a Node.js-based OData v2 + v4 backend located in `/cap-bookshop`.
It is intended for dev-time and mandatory for running the unit tests.

The unit tests expect a locally built Docker image for the CAP bookshop backend
so build the image (unpublished, unconventional tag on purpose) before running the tests:

```bash
docker build -t camunda/sap-odata-connector/cap-bookshop odata-connector/cap-bookshop
```
