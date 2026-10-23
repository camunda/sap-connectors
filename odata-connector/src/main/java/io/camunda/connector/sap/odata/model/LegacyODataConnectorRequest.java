package io.camunda.connector.sap.odata.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.util.Map;

public record LegacyODataConnectorRequest(
    @NotEmpty String destination,
    @NotEmpty @Pattern(regexp = "^([/=]).*", message = "oDataService must start with '/' or '='")
        String oDataService,
    @NotEmpty @Pattern(regexp = "^[^/].*$", message = "entityOrEntitySet must not start with a '/'")
        String entityOrEntitySet,
    @NotNull @Valid HttpMethod httpMethod,
    Map<String, Object> payload) {

  public ODataConnectorRequest toODataConnectorRequest() {
    return new ODataConnectorRequest(
        destination,
        oDataService,
        new ODataRequestDetails.SimpleRequest(entityOrEntitySet, httpMethod, payload));
  }
}
