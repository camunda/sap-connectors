package io.camunda.connector.sap.odata;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.camunda.connector.api.annotation.OutboundConnector;
import io.camunda.connector.api.outbound.OutboundConnectorFunction;
import io.camunda.connector.runtime.test.outbound.OutboundConnectorContextBuilder;
import io.camunda.connector.sap.odata.model.HttpMethod;
import io.camunda.connector.sap.odata.model.LegacyODataConnectorRequest;
import io.camunda.connector.sap.odata.model.ODataRequestDetails.SimpleRequest;
import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class LegacyODataCompatibilityTest {

  private static Map<String, Object> legacyGet() {
    return Map.of(
        "destination", "{{secrets.BOOKSHOP_DESTINATION}}",
        "oDataService", "/admin",
        "entityOrEntitySet", "Authors",
        "httpMethod",
            Map.of(
                "httpMethod", "get",
                "filter", "name eq 'Edgar Allen Poe'",
                "oDataVersionGet", Map.of("oDataVersionGet", "V4", "count", true)));
  }

  @Test
  void binds_legacy_get_with_resolved_secret_and_typed_options() {
    var context =
        OutboundConnectorContextBuilder.create()
            .secret("BOOKSHOP_DESTINATION", "bookshop")
            .variables(legacyGet())
            .build();
    var request =
        context.bindVariables(LegacyODataConnectorRequest.class).toODataConnectorRequest();

    assertThat(request.destination()).isEqualTo("bookshop");
    assertThat(request.oDataService()).isEqualTo("/admin");
    assertThat(request.requestDetails())
        .isInstanceOfSatisfying(
            SimpleRequest.class,
            simple -> {
              assertThat(simple.entityOrEntitySet()).isEqualTo("Authors");
              assertThat(simple.httpMethod())
                  .isInstanceOfSatisfying(
                      HttpMethod.Get.class,
                      get -> {
                        assertThat(get.filter()).isEqualTo("name eq 'Edgar Allen Poe'");
                        assertThat(get.oDataVersionGet())
                            .isEqualTo(new HttpMethod.Get.ODataVersionGet.V4(null, true));
                      });
            });
  }

  @Test
  void preserves_legacy_post_payload_and_protocol() {
    var payload = Map.<String, Object>of("ID", 123, "name", "Legacy author");
    var input = new HashMap<>(legacyGet());
    input.put("httpMethod", Map.of("httpMethod", "post", "oDataVersionPost", "V2"));
    input.put("payload", payload);
    var context =
        OutboundConnectorContextBuilder.create()
            .secret("BOOKSHOP_DESTINATION", "bookshop")
            .variables(input)
            .build();
    var request =
        context.bindVariables(LegacyODataConnectorRequest.class).toODataConnectorRequest();

    assertThat(request.requestDetails())
        .isInstanceOfSatisfying(
            SimpleRequest.class,
            simple -> {
              assertThat(simple.httpMethod())
                  .isEqualTo(new HttpMethod.Post(HttpMethod.ODataVersion.V2));
              assertThat(simple.payload()).isEqualTo(payload);
            });
  }

  @ParameterizedTest
  @ValueSource(strings = {"entityOrEntitySet", "httpMethod"})
  void rejects_missing_legacy_fields(String field) {
    var input = new HashMap<>(legacyGet());
    input.remove(field);
    var context =
        OutboundConnectorContextBuilder.create()
            .secret("BOOKSHOP_DESTINATION", "bookshop")
            .variables(input)
            .build();

    assertThatThrownBy(() -> new ODataConnector().execute(context))
        .isInstanceOf(RuntimeException.class)
        .hasStackTraceContaining(field);
  }

  @Test
  void does_not_fall_back_for_malformed_v2_request_details() {
    var input = new HashMap<>(legacyGet());
    input.put("requestDetails", Map.of("requestType", "unsupported"));
    var context =
        OutboundConnectorContextBuilder.create()
            .secret("BOOKSHOP_DESTINATION", "bookshop")
            .variables(input)
            .build();

    assertThatThrownBy(() -> new ODataConnector().execute(context))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("unsupported")
        .hasMessageContaining("requestDetails");
  }

  @Test
  void discovers_both_workers_without_changing_the_v2_identity() {
    var providers =
        ServiceLoader.load(OutboundConnectorFunction.class).stream()
            .map(ServiceLoader.Provider::type)
            .toList();

    assertThat(providers).contains(ODataConnector.class, LegacyODataConnector.class);
    var current = ODataConnector.class.getAnnotation(OutboundConnector.class);
    assertThat(current.name()).isEqualTo("SAP_ODATA_CONNECTOR");
    assertThat(current.type()).isEqualTo("io.camunda:sap-odata:2");
    var legacy = LegacyODataConnector.class.getAnnotation(OutboundConnector.class);
    assertThat(legacy.name()).isEqualTo("SAP_ODATA_CONNECTOR_LEGACY");
    assertThat(legacy.type()).isEqualTo("io.camunda:sap:odata:outbound:");
  }
}
