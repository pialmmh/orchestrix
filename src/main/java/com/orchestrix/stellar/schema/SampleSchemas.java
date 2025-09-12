package com.orchestrix.stellar.schema;

import com.orchestrix.stellar.model.Kind;

import java.util.List;
import java.util.Map;

public final class SampleSchemas {
  private SampleSchemas() {}
  public static SchemaMeta personOrderInvoice() {
    return new SchemaMeta(
      Map.of(
        Kind.PERSON,  new TableMeta("person",  "p", List.of("id","name"), "id"),
        Kind.ORDER,   new TableMeta("orders",  "o", List.of("id","idPerson","total"), "id"),
        Kind.INVOICE, new TableMeta("invoice", "i", List.of("id","idOrder","status","amount"), "id")
      ),
      List.of(
        new JoinEdge(Kind.PERSON, Kind.ORDER,   "id", "idPerson"),
        new JoinEdge(Kind.ORDER,  Kind.INVOICE, "id", "idOrder")
      )
    );
  }
}
