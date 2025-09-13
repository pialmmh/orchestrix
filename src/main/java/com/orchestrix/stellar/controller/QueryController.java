package com.orchestrix.stellar.controller;

import com.orchestrix.stellar.exec.Runner;
import com.orchestrix.stellar.json.QueryParser;
import com.orchestrix.stellar.model.QueryNode;
import com.orchestrix.stellar.model.EntityModificationRequest;
import com.orchestrix.stellar.result.FlatRow;
import com.orchestrix.stellar.result.ResultTransformer;
import com.orchestrix.stellar.schema.OrchestrixSchema;
import com.orchestrix.stellar.schema.SchemaMeta;
import com.orchestrix.stellar.sql.MysqlQueryBuilder;
import com.orchestrix.stellar.sql.SqlPlan;
import com.orchestrix.stellar.service.EntityModificationService;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.SQLException;
import java.util.*;

/**
 * REST Controller for query and modification endpoints
 */
@RestController
@RequestMapping("/stellar")
@CrossOrigin(origins = "*")
@Slf4j
public class QueryController {
    
    @Autowired
    private DataSource dataSource;
    
    @Autowired
    private EntityModificationService modificationService;
    
    private final MysqlQueryBuilder queryBuilder;
    private final ResultTransformer resultTransformer;
    private final SchemaMeta schemaMeta;
    
    public QueryController() {
        // Initialize with Orchestrix schema
        this.schemaMeta = OrchestrixSchema.getSchema();
        this.queryBuilder = new MysqlQueryBuilder(schemaMeta);
        this.resultTransformer = new ResultTransformer(schemaMeta);
    }
    
    /**
     * Main query endpoint that accepts JSON queries from the frontend
     */
    @PostMapping("/query")
    public ResponseEntity<Map<String, Object>> executeQuery(@RequestBody Map<String, Object> jsonQuery) {
        log.info("Received query: {}", jsonQuery);
        
        try {
            // Parse JSON to QueryNode
            QueryNode query = QueryParser.parse(jsonQuery);
            log.info("Parsed query for kind: {}", query.kind);
            
            // Build SQL
            SqlPlan plan = queryBuilder.build(query);
            log.info("Generated SQL: {}", plan.sql());
            log.info("Parameters: {}", plan.params());
            
            // Execute query
            Runner runner = new Runner(dataSource);
            List<FlatRow> rows = runner.execute(plan);
            log.info("Query returned {} flat rows", rows.size());
            
            // Transform flat rows to hierarchical structure with clean field names
            List<Map<String, Object>> results = resultTransformer.transform(rows, query);
            
            log.info("Transformed to {} hierarchical objects", results.size());
            
            // Return results
            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", results,
                "count", results.size()
            ));
            
        } catch (IllegalArgumentException e) {
            log.warn("Bad request: {}", e.getMessage());
            String stackTrace = getStackTraceAsString(e);
            return ResponseEntity.badRequest()
                .body(Map.of(
                    "success", false,
                    "error", e.getMessage(),
                    "exception", e.getClass().getName(),
                    "stackTrace", stackTrace
                ));
                
        } catch (SQLException e) {
            log.error("Database error: {}", e.getMessage());
            String stackTrace = getStackTraceAsString(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "success", false,
                    "error", "Database error: " + e.getMessage(),
                    "sqlState", e.getSQLState() != null ? e.getSQLState() : "",
                    "errorCode", e.getErrorCode(),
                    "exception", e.getClass().getName(),
                    "stackTrace", stackTrace
                ));
                
        } catch (Exception e) {
            log.error("Unexpected error: {}", e.getMessage(), e);
            String stackTrace = getStackTraceAsString(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "success", false,
                    "error", "Internal server error: " + e.getMessage(),
                    "exception", e.getClass().getName(),
                    "stackTrace", stackTrace
                ));
        }
    }
    
    /**
     * Health check endpoint
     */
    // Commented out to avoid conflict with TestController health endpoint
    // @GetMapping("/health")
    // public ResponseEntity<Map<String, Object>> health() {
    //     return ResponseEntity.ok(Map.of(
    //         "status", "healthy",
    //         "service", "stellar-spring",
    //         "timestamp", new Date()
    //     ));
    // }
    
    /**
     * Endpoint to fetch specific entity by kind (for compatibility with frontend)
     */
    @PostMapping("/{kind}")
    public ResponseEntity<Map<String, Object>> executeQueryByKind(
            @PathVariable String kind, 
            @RequestBody Map<String, Object> jsonQuery) {
        // Add the kind to the query if not present
        if (!jsonQuery.containsKey("kind")) {
            jsonQuery.put("kind", kind);
        }
        return executeQuery(jsonQuery);
    }
    
    /**
     * Generic entity modification endpoint with lazy hierarchy building and caching
     * Supports INSERT, UPDATE, DELETE operations with nested entities in a single transaction
     */
    @PostMapping("/modify")
    public ResponseEntity<Map<String, Object>> modifyEntity(@RequestBody EntityModificationRequest request) {
        log.info("Received modification request for entity: {} with operation: {}", 
                request.getEntityName(), request.getOperation());
        
        try {
            // Process modification with lazy hierarchy building
            Map<String, Object> result = modificationService.processModification(request);
            
            log.info("Modification completed successfully");
            return ResponseEntity.ok(result);
            
        } catch (IllegalArgumentException e) {
            log.warn("Bad modification request: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(Map.of(
                    "success", false,
                    "error", e.getMessage(),
                    "exception", e.getClass().getName()
                ));
                
        } catch (SQLException e) {
            log.error("Database error during modification: {}", e.getMessage());
            String stackTrace = getStackTraceAsString(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "success", false,
                    "error", "Database error: " + e.getMessage(),
                    "sqlState", e.getSQLState() != null ? e.getSQLState() : "",
                    "errorCode", e.getErrorCode(),
                    "exception", e.getClass().getName(),
                    "stackTrace", stackTrace
                ));
                
        } catch (Exception e) {
            log.error("Unexpected error during modification: {}", e.getMessage(), e);
            String stackTrace = getStackTraceAsString(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "success", false,
                    "error", "Internal server error: " + e.getMessage(),
                    "exception", e.getClass().getName(),
                    "stackTrace", stackTrace
                ));
        }
    }
    
    /**
     * Get cache statistics for entity hierarchies
     */
    @GetMapping("/cache/stats")
    public ResponseEntity<Map<String, Object>> getCacheStatistics() {
        Map<String, Object> stats = modificationService.getCacheStatistics();
        return ResponseEntity.ok(stats);
    }
    
    /**
     * Clear entity hierarchy cache (for testing/maintenance)
     */
    @DeleteMapping("/cache/clear")
    public ResponseEntity<Map<String, Object>> clearCache() {
        modificationService.clearCache();
        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "Entity hierarchy cache cleared"
        ));
    }
    
    /**
     * Helper method to get full stack trace as string
     */
    private String getStackTraceAsString(Exception e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        return sw.toString();
    }
}