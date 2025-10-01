package com.orchestrix.stellar.schema;

import com.orchestrix.stellar.model.Kind;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public record SchemaMeta(Map<Kind, TableMeta> nodes, List<JoinEdge> edges) {
  public Optional<JoinEdge> edge(Kind parent, Kind child) {
    return edges.stream().filter(e -> e.parent()==parent && e.child()==child).findFirst();
  }
}
