package io.camunda.connector.sap.odata.model.batchType;

import java.util.HashMap;
import java.util.Map;

public class Options {
  private String format;
  private String top;
  private String filter;
  private String orderby;
  private String expand;
  private String select;
  private String inlinecount;

  public String getFormat() {
    return format;
  }

  public void setFormat(String format) {
    this.format = format;
  }

  public String getTop() {
    return top;
  }

  public void setTop(String top) {
    this.top = top;
  }

  public String getFilter() {
    return filter;
  }

  public void setFilter(String filter) {
    this.filter = filter;
  }

  public String getOrderby() {
    return orderby;
  }

  public void setOrderby(String orderby) {
    this.orderby = orderby;
  }

  public String getExpand() {
    return expand;
  }

  public void setExpand(String expand) {
    this.expand = expand;
  }

  public String getSelect() {
    return select;
  }

  public void setSelect(String select) {
    this.select = select;
  }

  public String getInlinecount() {
    return inlinecount;
  }

  public void setInlinecount(String inlinecount) {
    this.inlinecount = inlinecount;
  }

  public Map<String, String> asMap() {
    Map<String, String> params = new HashMap<>();
    if (format != null && !format.isEmpty()) params.put("$format", format);
    if (top != null && !top.isEmpty()) params.put("$top", top);
    if (filter != null && !filter.isEmpty()) params.put("$filter", filter);
    if (orderby != null && !orderby.isEmpty()) params.put("$orderby", orderby);
    if (expand != null && !expand.isEmpty()) params.put("$expand", expand);
    if (select != null && !select.isEmpty()) params.put("$select", select);
    if (inlinecount != null && !inlinecount.isEmpty()) params.put("$inlinecount", inlinecount);
    return params;
  }
}
