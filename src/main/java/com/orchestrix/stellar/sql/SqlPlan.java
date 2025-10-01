package com.orchestrix.stellar.sql;

import java.util.List;

public record SqlPlan(String sql, List<Object> params) {}
