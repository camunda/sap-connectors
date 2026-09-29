package io.camunda.connector.sap.odata.model.batchType;

import java.util.Map;

public class Request {
  public enum Method {
    GET("GET"),
    POST("POST"),
    PUT("PUT"),
    PATCH("PATCH"),
    DELETE("DELETE");

    private final String value;

    Method(String value) {
      this.value = value;
    }

    @Override
    public String toString() {
      return value;
    }
  }

  private Method method;
  private String resourcePath;
  // optional for GET
  private Options options;
  // only required for POST, PUT, PATCH
  //  private Payload payload;
  private Map<String, Object> payload;

  public Method getMethod() {
    return method;
  }

  public void setMethod(Method method) {
    this.method = method;
  }

  public String getResourcePath() {
    return resourcePath;
  }

  public void setResourcePath(String resourcePath) {
    this.resourcePath = resourcePath;
  }

  public Options getOptions() {
    return options;
  }

  public void setOptions(Options options) {
    this.options = options;
  }

  public Map<String, Object> getPayload() {
    return payload;
  }

  public void setPayload(Map<String, Object> payload) {
    this.payload = payload;
  }
}
