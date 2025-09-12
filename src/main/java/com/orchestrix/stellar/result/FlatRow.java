package com.orchestrix.stellar.result;

import java.util.Map;

public record FlatRow(Map<String,Object> col) {
  public Object get(String label) { return col.get(label); }
}
