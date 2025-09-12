package com.telcobright.orchestrix.dto.stellar;

import java.util.List;
import java.util.Map;

public class QueryNode {
    private String kind;
    private List<String> select;
    private Map<String, Object> criteria;
    private List<QueryNode> include;
    private PageInfo page;
    private boolean lazy;
    private String lazyLoadKey;

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public List<String> getSelect() {
        return select;
    }

    public void setSelect(List<String> select) {
        this.select = select;
    }

    public Map<String, Object> getCriteria() {
        return criteria;
    }

    public void setCriteria(Map<String, Object> criteria) {
        this.criteria = criteria;
    }

    public List<QueryNode> getInclude() {
        return include;
    }

    public void setInclude(List<QueryNode> include) {
        this.include = include;
    }

    public PageInfo getPage() {
        return page;
    }

    public void setPage(PageInfo page) {
        this.page = page;
    }

    public boolean isLazy() {
        return lazy;
    }

    public void setLazy(boolean lazy) {
        this.lazy = lazy;
    }

    public String getLazyLoadKey() {
        return lazyLoadKey;
    }

    public void setLazyLoadKey(String lazyLoadKey) {
        this.lazyLoadKey = lazyLoadKey;
    }

    public static class PageInfo {
        private int limit;
        private int offset;

        public int getLimit() {
            return limit;
        }

        public void setLimit(int limit) {
            this.limit = limit;
        }

        public int getOffset() {
            return offset;
        }

        public void setOffset(int offset) {
            this.offset = offset;
        }
    }
}