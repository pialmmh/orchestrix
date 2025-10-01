package com.orchestrix.stellar.schema;

import java.util.List;

public record TableMeta(String table, String alias, List<String> columns, String idColumn) {}
