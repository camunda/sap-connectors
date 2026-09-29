package io.camunda.connector.sap.odata.model.batchType;

/**
 * runtime equivalent of the connector template's batch request options see
 * src/test/resources/batch.json for an example
 */
public class BatchRequestRepresentation {
  public enum EntryKind {
    BATCH("batch"),
    CHANGESET("changeset");

    private final String value;

    EntryKind(String value) {
      this.value = value;
    }

    @Override
    public String toString() {
      return value;
    }
  }

  private EntryKind type;
  private Request[] requests;

  public EntryKind getType() {
    return type;
  }

  public void setType(EntryKind type) {
    this.type = type;
  }

  public Request[] getRequests() {
    return requests;
  }

  public void setRequests(Request[] requests) {
    this.requests = requests;
  }
}
