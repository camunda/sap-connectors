package io.camunda.connector.sap.odata;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.sap.cloud.sdk.cloudplatform.connectivity.AuthenticationType;
import com.sap.cloud.sdk.cloudplatform.connectivity.DefaultHttpDestination;
import com.sap.cloud.sdk.cloudplatform.connectivity.DestinationAccessor;
import io.camunda.connector.api.annotation.OutboundConnector;
import io.camunda.connector.api.error.ConnectorInputException;
import io.camunda.connector.api.outbound.OutboundConnectorFunction;
import io.camunda.connector.jackson.ConnectorsObjectMapperSupplier;
import io.camunda.connector.runtime.test.outbound.OutboundConnectorContextBuilder;
import io.camunda.connector.sap.odata.model.HttpMethod.ODataVersion;
import io.camunda.connector.sap.odata.model.ODataConnectorBatchResponse;
import io.camunda.connector.sap.odata.model.ODataConnectorRequest;
import io.camunda.connector.sap.odata.model.ODataConnectorResponse;
import io.camunda.connector.sap.odata.model.ODataRequestDetails.BatchRequest;
import io.vavr.control.Try;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
@ResourceLock("DestinationAccessor")
class ODataMaintenanceTest {
  @Container
  private static final GenericContainer<?> cap =
      new GenericContainer<>(DockerImageName.parse("camunda/sap-odata-connector/cap-bookshop"))
          .withEnv("CDS_QUERY_LIMIT_DEFAULT", "1")
          .withEnv("CDS_QUERY_LIMIT_MAX", "1")
          .withExposedPorts(4004)
          .waitingFor(Wait.forHttp("/health").forStatusCode(200));

  @BeforeEach
  void configureDestination() {
    DestinationAccessor.setLoader(null);
    var destination =
        DefaultHttpDestination.builder(baseUrl())
            .authenticationType(AuthenticationType.BASIC_AUTHENTICATION)
            .basicCredentials("alice", "password")
            .build();
    DestinationAccessor.prependDestinationLoader((name, options) -> Try.success(destination));
  }

  @AfterEach
  void resetDestination() {
    DestinationAccessor.setLoader(null);
  }

  private static String baseUrl() {
    return "http://" + cap.getHost() + ":" + cap.getFirstMappedPort();
  }

  private static HttpResponse<String> fixtureRequest(String method, String path, String body)
      throws Exception {
    try (var client = HttpClient.newHttpClient()) {
      return client.send(
          HttpRequest.newBuilder(URI.create(baseUrl() + path))
              .header(
                  "Authorization",
                  "Basic "
                      + Base64.getEncoder()
                          .encodeToString("alice:password".getBytes(StandardCharsets.UTF_8)))
              .header("Content-Type", "application/json")
              .method(method, HttpRequest.BodyPublishers.ofString(body))
              .build(),
          HttpResponse.BodyHandlers.ofString());
    }
  }

  private static ODataConnectorBatchResponse batch(
      ODataVersion protocol, String service, String kind, List<Map<String, Object>> requests) {
    var input =
        new ODataConnectorRequest(
            "bookshop",
            service,
            new BatchRequest(protocol, List.of(Map.of("type", kind, "requests", requests))));
    return (ODataConnectorBatchResponse)
        new ODataConnector()
            .execute(OutboundConnectorContextBuilder.create().variables(input).build());
  }

  @ParameterizedTest
  @CsvSource({"V2,/odata/v2/admin", "V4,/admin"})
  void keeps_batch_entity_envelope(ODataVersion protocol, String service) {
    var result =
        batch(
            protocol,
            service,
            "batch",
            List.of(Map.of("method", "GET", "resourcePath", "Authors(150)")));
    assertThat(result.batchResponses()).hasSize(1);
    var success = (ODataConnectorResponse) result.batchResponses().getFirst();
    assertThat(success.statusCode()).isEqualTo(200);
    var entity = protocol == ODataVersion.V2 ? success.result().path("d") : success.result();
    assertThat(entity.path("name").asText()).isEqualTo("Edgar Allen Poe");
  }

  @ParameterizedTest
  @CsvSource({"V2,/odata/v2/admin", "V4,/admin"})
  void keeps_individual_batch_errors(ODataVersion protocol, String service) {
    var result =
        batch(
            protocol,
            service,
            "batch",
            List.of(Map.of("method", "GET", "resourcePath", "Authors(9999999)")));
    assertThat(result.batchResponses()).hasSize(1);
    var error = (ODataConnectorResponse) result.batchResponses().get(0);
    assertThat(error.statusCode()).isEqualTo(404);
    assertThat(error.result().path("target").asText()).contains("Authors");
    assertThat(error.result().path("odata").has("error")).isTrue();
  }

  @Test
  void keeps_v2_batch_results_and_opaque_continuation_token() throws Exception {
    var direct = fixtureRequest("GET", "/odata/v2/admin/Authors", "");
    assertThat(direct.statusCode()).isEqualTo(200);
    var expected = ConnectorsObjectMapperSupplier.getCopy().readTree(direct.body()).path("d");
    assertThat(expected.path("results")).hasSize(1);
    assertThat(expected.path("__next").asText()).contains("$skiptoken");
    var result =
        batch(
            ODataVersion.V2,
            "/odata/v2/admin",
            "batch",
            List.of(Map.of("method", "GET", "resourcePath", "Authors")));
    var response = (ODataConnectorResponse) result.batchResponses().getFirst();
    assertThat(response.statusCode()).isEqualTo(200);
    assertThat(response.result().path("d")).isEqualTo(expected);
  }

  @ParameterizedTest
  @CsvSource({"V2,/odata/v2/admin", "V4,/admin"})
  void returns_success_for_empty_204_after_committed_batch_delete(
      ODataVersion protocol, String service) throws Exception {
    int id = ThreadLocalRandom.current().nextInt(1_000_000, Integer.MAX_VALUE);
    var created =
        fixtureRequest(
            "POST", "/admin/Authors", "{\"ID\":" + id + ",\"name\":\"batch regression\"}");
    assertThat(created.statusCode()).isEqualTo(201);
    ODataConnectorBatchResponse result;
    try {
      result =
          batch(
              protocol,
              service,
              "changeset",
              List.of(Map.of("method", "DELETE", "resourcePath", "Authors(" + id + ")")));
    } finally {
      assertThat(fixtureRequest("GET", "/admin/Authors(" + id + ")", "").statusCode())
          .as("independent read confirms that the server committed the delete")
          .isEqualTo(404);
    }
    assertThat(result.batchResponses())
        .singleElement()
        .isInstanceOfSatisfying(
            ODataConnectorResponse.class,
            response -> assertThat(response.statusCode()).isEqualTo(204));
  }

  @ParameterizedTest
  @ValueSource(strings = {"missing", "null", "rootPayload", "nestedPayload"})
  void rejects_missing_details_and_unsupported_flat_requests(String format) {
    var input = new HashMap<String, Object>();
    input.put("destination", "bookshop");
    input.put("oDataService", "/admin");
    if (format.equals("null")) {
      input.put("requestDetails", null);
    } else if (!format.equals("missing")) {
      input.put("entityOrEntitySet", "Authors");
      input.put(
          "httpMethod",
          Map.of("httpMethod", "post", "oDataVersionPost", "V4", "payloadPost", Map.of("ID", 1)));
      if (format.equals("rootPayload")) {
        input.put("payload", Map.of("ID", 1));
      }
    }
    var context = OutboundConnectorContextBuilder.create().variables(input).build();
    assertThatThrownBy(() -> new ODataConnector().execute(context))
        .isInstanceOf(ConnectorInputException.class)
        .hasMessageContaining("requestDetails");
  }

  @ParameterizedTest
  @ValueSource(strings = {"{}", "[]", "\"text\"", "{\"requestType\":\"unsupported\"}"})
  void rejects_malformed_request_details(String json) throws Exception {
    var details = ConnectorsObjectMapperSupplier.getCopy().readTree(json);
    var context =
        OutboundConnectorContextBuilder.create()
            .variables(
                Map.of(
                    "destination", "bookshop", "oDataService", "/admin", "requestDetails", details))
            .build();
    assertThatThrownBy(() -> new ODataConnector().execute(context))
        .hasCauseInstanceOf(JsonMappingException.class)
        .hasStackTraceContaining("requestDetails");
  }

  @Test
  void registers_only_the_supported_worker_type() {
    var types =
        ServiceLoader.load(OutboundConnectorFunction.class).stream()
            .map(ServiceLoader.Provider::type)
            .filter(type -> type.getPackageName().equals(getClass().getPackageName()))
            .map(type -> type.getAnnotation(OutboundConnector.class).type())
            .toList();
    assertThat(types).containsExactly("io.camunda:sap-odata:2");
  }
}
