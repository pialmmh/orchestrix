package com.telcobright.orchestrix.controller;

import com.telcobright.stellar.mutation.*;
import com.telcobright.stellar.model.QueryNodeV2;
import com.telcobright.stellar.sql.*;
import com.telcobright.stellar.result.*;
import com.telcobright.stellar.schema.SchemaMetaV2;
import com.telcobright.stellar.json.QueryParserV2;
import com.telcobright.stellar.exec.Runner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/stellar")
@CrossOrigin(origins = "*")
public class StellarV2Controller {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private SchemaMetaV2 schemaV2;

    @Autowired
    private MutationExecutor mutationExecutor;

    @Autowired
    private MysqlQueryBuilderV2 queryBuilderV2;

    @Autowired
    private ResultTransformerV2 resultTransformerV2;
    
    @PostMapping("/query")
    public ResponseEntity<Map<String, Object>> query(@RequestBody Map<String, Object> request) {
        try (Connection conn = dataSource.getConnection()) {
            // Parse JSON to QueryNodeV2
            QueryNodeV2 query = QueryParserV2.parse(request);

            // Generate SQL
            SqlPlan plan = queryBuilderV2.build(query);

            // Execute query
            List<FlatRow> flatRows = new ArrayList<>();
            try (PreparedStatement stmt = conn.prepareStatement(plan.sql())) {
                for (int i = 0; i < plan.params().size(); i++) {
                    stmt.setObject(i + 1, plan.params().get(i));
                }

                try (ResultSet rs = stmt.executeQuery()) {
                    ResultSetMetaData metaData = rs.getMetaData();
                    while (rs.next()) {
                        Map<String, Object> row = new LinkedHashMap<>();
                        for (int col = 1; col <= metaData.getColumnCount(); col++) {
                            row.put(metaData.getColumnLabel(col), rs.getObject(col));
                        }
                        flatRows.add(new FlatRow(row));
                    }
                }
            }

            // Transform to hierarchical structure with clean field names
            List<Map<String, Object>> results = resultTransformerV2.transform(flatRows, query);

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
    
    // Delete endpoint
    @DeleteMapping("/{entity}/{id}")
    public ResponseEntity<?> delete(
            @PathVariable String entity,
            @PathVariable Object id) {
        try (Connection conn = dataSource.getConnection()) {
            MutationRequest request = MutationRequest.deleteById(entity, id);
            MutationResult result = mutationExecutor.execute(conn, request);

            if (result.isSuccess()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("affectedRows", result.affectedRows());
                response.put("message", "Entity deleted successfully");
                return ResponseEntity.ok(response);
            }
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", result.message()
            ));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }

    // Create endpoint
    @PostMapping("/{entity}")
    public ResponseEntity<?> create(
            @PathVariable String entity,
            @RequestBody Map<String, Object> data) {
        try (Connection conn = dataSource.getConnection()) {
            MutationRequest request = MutationRequest.create(entity, data);
            MutationResult result = mutationExecutor.execute(conn, request);

            if (result.isSuccess()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("data", result.data());
                response.put("message", result.message());
                return ResponseEntity.ok(response);
            }
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", result.message()
            ));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }

    // Update endpoint
    @PutMapping("/{entity}/{id}")
    public ResponseEntity<?> update(
            @PathVariable String entity,
            @PathVariable Object id,
            @RequestBody Map<String, Object> data) {
        try (Connection conn = dataSource.getConnection()) {
            MutationRequest request = MutationRequest.updateById(entity, id, data);
            MutationResult result = mutationExecutor.execute(conn, request);

            if (result.isSuccess()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("data", result.data());
                response.put("message", result.message());
                return ResponseEntity.ok(response);
            }
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", result.message()
            ));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }

    // Legacy mutate endpoint for compatibility
    @PostMapping("/mutate")
    public ResponseEntity<?> mutate(@RequestBody Map<String, Object> request) {
        String entityName = (String) request.get("entityName");
        String operation = (String) request.get("operation");
        Object id = request.get("id");
        Map<String, Object> data = (Map<String, Object>) request.get("data");

        switch (operation) {
            case "INSERT":
            case "CREATE":
                return create(entityName, data != null ? data : request);
            case "UPDATE":
                return update(entityName, id, data != null ? data : request);
            case "DELETE":
                return delete(entityName, id);
            default:
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "Unknown operation: " + operation
                ));
        }
    }
}