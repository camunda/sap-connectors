### SAP Business Partner Search - Demo Blueprint

Accelerate SAP S/4HANA and ECC project integrations.

This blueprint provides a ready-to-use, configurable process for searching and displaying business partner data from various SAP systems through Camunda.
Select your SAP system, enter filters in a Camunda form, and view live Business Partner data directly in a native Fiori environment, all without writing custom code.  


⚠️ **Important**: The sample values provided in the template are for demonstration purposes only.
You must replace them with your own query parameters and business logic as needed.

Refer to the [integration documentation](https://docs.camunda.io/docs/components/camunda-integrations/sap/sap-integration/) for more details.  

<hr/>


#### How does the process work?

1. **Start Event:** The process is initiated, starting the search for a business partner.

2. **Select System:** A user task prompts the user to decide which SAP system to use.

3. **Mix & Match:** A placeholder task highlights where you can mix BPMN tasks with SAP tasks.

4. **Search Screen:** The user is presented with a form in a native Fiori experience to input their search criteria.

5. **Exclusive Gateway:** The process branches based on the system selected earlier.
If S/4HANA: An OData Connector queries the S/4HANA system for the business partner.
If ECC: An RFC Connector queries the ECC system for cost centers (as an example).

6. **Error Handling:** If the query or connection fails, a user task displays the relevant error message before terminating the process.

7. **Result Screen:** If the query is successful, the results are displayed to the user.

8. **New Search?:** A gateway asks the user if they want to perform another search.
If yes: The process loops back to the "Search Screen".
If no: The process concludes.

<hr/>

[View blueprint on the Camunda Marketplace](https://marketplace.camunda.com/en-US/apps/597911/sap-business-partner-search)