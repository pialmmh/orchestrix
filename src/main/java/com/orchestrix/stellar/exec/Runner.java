package com.orchestrix.stellar.exec;

import com.orchestrix.stellar.result.FlatRow;
import com.orchestrix.stellar.sql.SqlPlan;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.Function;

public final class Runner {
  private final DataSource ds;
  public Runner(DataSource ds) { this.ds = ds; }

  public List<FlatRow> execute(SqlPlan plan) throws SQLException {
    return execute(plan, row -> row);
  }

  public <T> List<T> execute(SqlPlan plan, Function<FlatRow,T> mapper) throws SQLException {
    try (var conn = ds.getConnection();
         var ps = conn.prepareStatement(plan.sql())) {
      int i=1;
      for (Object p : plan.params()) ps.setObject(i++, p);
      try (var rs = ps.executeQuery()) {
        var md = rs.getMetaData();
        int n = md.getColumnCount();
        var out = new ArrayList<T>();
        while (rs.next()) {
          var map = new LinkedHashMap<String,Object>(n);
          for (int c=1; c<=n; c++) {
            map.put(md.getColumnLabel(c), rs.getObject(c));
          }
          out.add(mapper.apply(new FlatRow(map)));
        }
        return out;
      }
    }
  }
}
