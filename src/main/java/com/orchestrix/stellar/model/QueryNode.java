package com.orchestrix.stellar.model;

import java.util.ArrayList;
import java.util.List;

public final class QueryNode {
  public final Kind kind;
  public final Criteria criteria;
  public final Page page;
  public final List<QueryNode> include;
  public final boolean lazy;
  public final String lazyLoadKey; // Unique key for lazy load reference

  private QueryNode(Kind kind, Criteria criteria, Page page, List<QueryNode> include, boolean lazy, String lazyLoadKey) {
    this.kind = kind; this.criteria = criteria; this.page = page;
    this.include = include == null ? List.of() : List.copyOf(include);
    this.lazy = lazy;
    this.lazyLoadKey = lazyLoadKey;
  }
  public static Builder of(Kind kind) { return new Builder(kind); }

  public static final class Builder {
    private final Kind kind;
    private Criteria criteria;
    private Page page;
    private final List<QueryNode> include = new ArrayList<>();
    private boolean lazy = false;
    private String lazyLoadKey = null;
    public Builder(Kind kind) { this.kind = kind; }
    public Builder criteria(Criteria criteria) { this.criteria = criteria; return this; }
    public Builder page(Page page) { this.page = page; return this; }
    public Builder include(QueryNode... nodes) { this.include.addAll(List.of(nodes)); return this; }
    public Builder lazy(boolean lazy) { this.lazy = lazy; return this; }
    public Builder lazyLoadKey(String key) { this.lazyLoadKey = key; return this; }
    public QueryNode build() { return new QueryNode(kind, criteria, page, include, lazy, lazyLoadKey); }
  }
}
