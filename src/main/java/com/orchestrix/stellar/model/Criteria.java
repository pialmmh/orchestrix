package com.orchestrix.stellar.model;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record Criteria(Map<String, List<Object>> fields) {
  public static Criteria of(String key, Object... vals) {
    return new Criteria(Map.of(key, List.of(vals)));
  }
  public Criteria and(String key, Object... vals) {
    var copy = new LinkedHashMap<>(fields);
    copy.put(key, List.of(vals));
    return new Criteria(copy);
  }
  public boolean isEmpty() { return fields == null || fields.isEmpty(); }
}
