package io.camunda.connector.sap.rfc;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.camunda.zeebe.client.CredentialsProvider;
import io.camunda.zeebe.client.ZeebeClient;
import java.net.URI;
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

  ZeebeClient zeebeClient;

  E2eTest() {
    // the evn vars are set in the github action
    // derived from the repo secrets
    zeebeClient =
        ZeebeClient.newClientBuilder()
            .grpcAddress(URI.create(System.getenv("GRPC_ADDRESS")))
            .restAddress(URI.create(System.getenv("REST_ADDRESS")))
            .credentialsProvider(
                CredentialsProvider.newCredentialsProviderBuilder()
                    .audience("zeebe.ultrawombat.com")
                    .clientId(System.getenv("CLIENT_ID"))
                    .clientSecret(System.getenv("CLIENT_SECRET"))
                    .authorizationServerUrl("https://login.cloud.ultrawombat.com/oauth/token")
                    .build())
            .build();
  }

  @Test
  void bapi() {
    zeebeClient.newDeployResourceCommand().addResourceFromClasspath("bapi.bpmn").send().join();

    var processInstanceResult =
        zeebeClient
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
    zeebeClient.newDeployResourceCommand().addResourceFromClasspath("rfm.bpmn").send().join();

    var processInstanceResult =
        zeebeClient
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
    zeebeClient
        .newDeployResourceCommand()
        .addResourceFromClasspath("rfm-atomic-values.bpmn")
        .send()
        .join();

    var processInstanceResult =
        zeebeClient
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
