package com.telcobright.orchestrix.controller;

import com.telcobright.stellar.model.QueryNodeV2;
import com.telcobright.stellar.sql.MysqlQueryBuilderV2;
import com.telcobright.stellar.sql.SqlPlan;
import com.telcobright.stellar.result.ResultTransformerV2;
import com.telcobright.stellar.result.FlatRow;
import com.telcobright.stellar.schema.SchemaMetaV2;
import com.telcobright.stellar.json.QueryParserV2;
import com.telcobright.stellar.exec.Runner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/v2/stellar")
@CrossOrigin(origins = "*")
public class StellarV2Controller {
    
    private final DataSource dataSource;
    private final SchemaMetaV2 schema;
    private final MysqlQueryBuilderV2 queryBuilder;
    private final ResultTransformerV2 transformer;
    
    @Autowired
    public StellarV2Controller(DataSource dataSource, SchemaMetaV2 schema) {
        this.dataSource = dataSource;
        this.schema = schema;
        this.queryBuilder = new MysqlQueryBuilderV2(schema);
        this.transformer = new ResultTransformerV2(schema);
    }
    
    @PostMapping("/query")
    public ResponseEntity<Map<String, Object>> query(@RequestBody Map<String, Object> request) {
        try {
            // Parse JSON to QueryNodeV2
            QueryNodeV2 query = QueryParserV2.parse(request);
            
            // Generate SQL
            SqlPlan plan = queryBuilder.build(query);
            
            // Execute query
            Runner runner = new Runner(dataSource);
            List<FlatRow> rows = runner.execute(plan);
            
            // Transform to hierarchical structure with clean field names
            List<Map<String, Object>> results = transformer.transform(rows, query);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", results);
            response.put("count", results.size());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            errorResponse.put("data", List.of());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
    
    @PostMapping("/mutate")
    public ResponseEntity<Map<String, Object>> mutate(@RequestBody Map<String, Object> request) {
        // TODO: Implement mutation support if needed
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("error", "Mutations not yet implemented in V2");
        return ResponseEntity.status(501).body(response);
    }
}