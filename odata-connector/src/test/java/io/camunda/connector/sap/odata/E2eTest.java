package io.camunda.connector.sap.odata;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.camunda.client.CamundaClient;
import java.time.Duration;
import java.util.LinkedHashMap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Depends on the public SAP OData service https://services.odata.org/V2.
@EnabledIfEnvironmentVariable(named = "e2e", matches = "true")
public class E2eTest {

  private static final Logger LOGGER = LoggerFactory.getLogger(E2eTest.class);

  CamundaClient camundaClient;

  E2eTest() {
    // the env vars are set in the GitHub Action
    // derived from the repo secrets
    camundaClient =
        CamundaClient.newCloudClientBuilder()
            .withClusterId(System.getenv("CLUSTER_ID"))
            .withClientId(System.getenv("CLIENT_ID"))
            .withClientSecret(System.getenv("CLIENT_SECRET"))
            .withRegion(System.getenv("REGION_ID"))
            .withDomain(System.getenv("BASE_DOMAIN"))
            .build();
  }

  @Test
  void odata_service_get_products() {
    camundaClient
        .newDeployResourceCommand()
        .addResourceFromClasspath("odata-service-get-products.bpmn")
        .send()
        .join();

    var processInstanceResult =
        camundaClient
            .newCreateInstanceCommand()
            .bpmnProcessId("odata-service-get-products")
            .latestVersion()
            .withResult()
            .requestTimeout(Duration.ofSeconds(30))
            .send()
            .join();

    var result = processInstanceResult.getVariablesAsMap();
    LinkedHashMap products = (LinkedHashMap) result.get("products");
    LOGGER.info("//> products result size: " + products.size());
    assertTrue(products.size() >= 1, products.size() + " products returned");
  }
}
