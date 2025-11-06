package io.camunda.connector.sap.rfc;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.camunda.client.CamundaClient;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@EnabledIfEnvironmentVariable(named = "e2e", matches = "true")
public class E2eTest {

  private static final Logger LOGGER = LoggerFactory.getLogger(E2eTest.class);

  CamundaClient camundaClient;

  E2eTest() {
    // the env vars are set in the GitHub action
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
  void bapi() {
    camundaClient.newDeployResourceCommand().addResourceFromClasspath("bapi.bpmn").send().join();

    var processInstanceResult =
        camundaClient
            .newCreateInstanceCommand()
            .bpmnProcessId("bapi")
            .latestVersion()
            .withResult()
            .requestTimeout(java.time.Duration.ofSeconds(30))
            .send()
            .join();

    var result = processInstanceResult.getVariablesAsMap();
    LinkedHashMap costCenter = (LinkedHashMap) result.get("CostCenter");
    LinkedHashMap tables = (LinkedHashMap) costCenter.get("tables");
    int costCenterListSize = ((ArrayList) tables.get("COSTCENTERLIST")).size();
    LOGGER.info("//> cost center result size: " + costCenterListSize);
    assertTrue(costCenterListSize >= 1, costCenterListSize + " cost center returned");
  }

  @Test
  void rfm_collection_result() {
    camundaClient.newDeployResourceCommand().addResourceFromClasspath("rfm.bpmn").send().join();

    var processInstanceResult =
        camundaClient
            .newCreateInstanceCommand()
            .bpmnProcessId("rfm")
            .latestVersion()
            .withResult()
            .requestTimeout(java.time.Duration.ofSeconds(30))
            .send()
            .join();

    var result = processInstanceResult.getVariablesAsMap();
    LinkedHashMap tableEntries = (LinkedHashMap) result.get("TableEntries");
    LinkedHashMap tables = (LinkedHashMap) tableEntries.get("tables");
    int tableEntriesSize = ((ArrayList) tables.get("ENTRIES")).size();
    LOGGER.info("//> table entries size: " + tableEntriesSize);
    assertTrue(tableEntriesSize >= 1, tableEntriesSize + " table entries returned");
  }

  @Test
  void rfm_atomic_result() {
    camundaClient
        .newDeployResourceCommand()
        .addResourceFromClasspath("rfm-atomic-values.bpmn")
        .send()
        .join();

    var processInstanceResult =
        camundaClient
            .newCreateInstanceCommand()
            .bpmnProcessId("rfm-atomic-result")
            .latestVersion()
            .withResult()
            .requestTimeout(Duration.ofSeconds(30))
            .send()
            .join();

    var result = processInstanceResult.getVariablesAsMap();
    LinkedHashMap importing =
        (LinkedHashMap) ((LinkedHashMap) result.get("sys_params")).get("importing");
    int size = importing.size();
    LOGGER.info("//> return params ('importing') entries size: " + size);
    assertTrue(size >= 5, size + " table entries returned");
  }
}
