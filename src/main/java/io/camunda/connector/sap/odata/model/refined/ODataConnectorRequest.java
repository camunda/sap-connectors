package io.camunda.connector.sap.odata.model.refined;

import io.camunda.connector.generator.dsl.Property.FeelMode;
import io.camunda.connector.generator.java.annotation.TemplateProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;

public record ODataConnectorRequest(
    @TemplateProperty(
        group = "sap",
        label = "BTP destination name",
        description = "BTP destination pointing to the SAP System to connect to (e.g. a4h)",
        feel = FeelMode.optional)
    @NotEmpty
    String destination,

    @TemplateProperty(
        group = "sap",
        label = "OData base service path",
        description = "absolute base path, e.g. /sap/opu/odata/dmo/case",
        feel = FeelMode.optional)
    @Pattern(regexp = "^([/=]).*", message = "oDataService must start with a '/'")
    @NotEmpty
    String oDataService,

    @Valid ODataRequestDetails requestDetails
) {}
