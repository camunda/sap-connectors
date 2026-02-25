package io.camunda.connector.sap.odata;

import io.camunda.connector.api.annotation.OutboundConnector;
import io.camunda.connector.api.outbound.OutboundConnectorContext;
import io.camunda.connector.api.outbound.OutboundConnectorFunction;
import io.camunda.connector.sap.odata.model.FallbackODataConnectorRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Connector worker for the legacy v1 task type {@code io.camunda:sap:odata:outbound:}.
 *
 * <p>Older element templates (pre-v2) used this task type with a flat variable structure — no
 * {@code requestDetails} wrapper. This connector binds directly to {@link
 * FallbackODataConnectorRequest}, converts to the canonical {@link
 * io.camunda.connector.sap.odata.model.ODataConnectorRequest}, and delegates to the standard
 * executor.
 */
@OutboundConnector(
    name = LegacyODataConnector.NAME,
    inputVariables = {},
    type = LegacyODataConnector.TYPE)
public class LegacyODataConnector implements OutboundConnectorFunction {
  private static final Logger LOGGER = LoggerFactory.getLogger(LegacyODataConnector.class);

  public static final String NAME = "SAP_ODATA_CONNECTOR_LEGACY";
  public static final String TYPE = "io.camunda:sap:odata:outbound:";

  @Override
  public Object execute(OutboundConnectorContext context) {
    LOGGER.info("Legacy v1 task type '{}' — binding flat variables via fallback model", TYPE);
    var fallback = context.bindVariables(FallbackODataConnectorRequest.class);
    var request = fallback.toODataConnectorRequest();

    ODataRequestExecutor requestExecutor = new ODataRequestExecutor();
    return requestExecutor.executeRequest(request);
  }
}
