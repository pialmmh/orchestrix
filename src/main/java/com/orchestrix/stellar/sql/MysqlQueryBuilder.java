package com.orchestrix.stellar.sql;

import com.orchestrix.stellar.model.QueryNode;
import com.orchestrix.stellar.schema.JoinEdge;
import com.orchestrix.stellar.schema.SchemaMeta;
import com.orchestrix.stellar.schema.TableMeta;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public final class MysqlQueryBuilder {
  private final SchemaMeta meta;
  public MysqlQueryBuilder(SchemaMeta meta) { this.meta = meta; }

  public SqlPlan build(QueryNode root) {
    var ctx = new Ctx();
    String fromChain = buildFromChain(root, null, ctx);
    String selectList = String.join(", ", ctx.projections);
    String sql = "SELECT " + selectList + " FROM " + fromChain + ";";
    return new SqlPlan(sql, ctx.params);
  }

  private static final class Ctx {
    final List<String> projections = new ArrayList<>();
    final List<Object> params = new ArrayList<>();
  }

  private String buildFromChain(QueryNode node, QueryNode parent, Ctx ctx) {
    TableMeta t = meta.nodes().get(node.kind);
    if (t == null) throw new IllegalStateException("Unknown kind: " + node.kind);

    final String baseAlias = t.alias();
    final String subAlias  = baseAlias + "_sub";

    for (String col : t.columns()) {
      ctx.projections.add(subAlias + ".`" + col + "` AS `" + baseAlias + "__" + col + "`");
    }

    String cols = t.columns().stream().map(c -> "`"+c+"`").collect(Collectors.joining(", "));
    StringBuilder sub = new StringBuilder();
    sub.append("(")
       .append("SELECT ").append(cols)
       .append(" FROM `").append(t.table()).append("`");

    List<String> wh = new ArrayList<>();
    if (node.criteria != null && !node.criteria.isEmpty()) {
      for (var e : node.criteria.fields().entrySet()) {
        String placeholders = String.join(", ", Collections.nCopies(e.getValue().size(), "?"));
        wh.add("`" + e.getKey() + "` IN (" + placeholders + ")");
        ctx.params.addAll(e.getValue());
      }
    }
    if (!wh.isEmpty()) sub.append(" WHERE ").append(String.join(" AND ", wh));

    if (node.page != null) {
      sub.append(" LIMIT ").append(node.page.offset()).append(",").append(node.page.limit());
    }
    sub.append(") AS ").append(subAlias);

    if (parent != null) {
      TableMeta pt = meta.nodes().get(parent.kind);
      JoinEdge edge = meta.edge(parent.kind, node.kind)
          .orElseThrow(() -> new IllegalStateException("No edge " + parent.kind + " -> " + node.kind));
      String parentSub = pt.alias() + "_sub";
      return " LEFT JOIN " + sub + " ON " + parentSub + ".`" + edge.parentKey() + "` = " + subAlias + ".`" + edge.childFk() + "`";
    }

    String chain = sub.toString();
    for (QueryNode child : node.include) {
      // Skip lazy-loaded children in the main query
      if (!child.lazy) {
        chain = chain + buildFromChain(child, node, ctx);
      }
    }
    return chain;
  }
}
