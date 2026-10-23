package io.camunda.connector.sap.odata;

import io.camunda.connector.api.annotation.OutboundConnector;
import io.camunda.connector.api.outbound.OutboundConnectorContext;
import io.camunda.connector.api.outbound.OutboundConnectorFunction;

@OutboundConnector(
    name = LegacyODataConnector.NAME,
    inputVariables = {},
    type = LegacyODataConnector.TYPE)
public class LegacyODataConnector implements OutboundConnectorFunction {
  public static final String NAME = "SAP_ODATA_CONNECTOR_LEGACY";
  public static final String TYPE = "io.camunda:sap:odata:outbound:";

  @Override
  public Object execute(OutboundConnectorContext context) {
    return new ODataConnector().execute(context);
  }
}
