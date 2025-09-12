package com.orchestrix.stellar.result;

import java.util.Map;

/**
 * Represents lazy load information for a query node
 */
public class LazyLoadInfo {
    private final String lazyLoadKey;
    private final String parentKind;
    private final String childKind;
    private final Map<String, Object> parentCriteria;
    
    public LazyLoadInfo(String lazyLoadKey, String parentKind, String childKind, Map<String, Object> parentCriteria) {
        this.lazyLoadKey = lazyLoadKey;
        this.parentKind = parentKind;
        this.childKind = childKind;
        this.parentCriteria = parentCriteria;
    }
    
    public String getLazyLoadKey() { return lazyLoadKey; }
    public String getParentKind() { return parentKind; }
    public String getChildKind() { return childKind; }
    public Map<String, Object> getParentCriteria() { return parentCriteria; }
}