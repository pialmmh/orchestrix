package com.orchestrix.stellar.schema;

import com.orchestrix.stellar.model.Kind;

public record JoinEdge(Kind parent, Kind child, String parentKey, String childFk) {}
