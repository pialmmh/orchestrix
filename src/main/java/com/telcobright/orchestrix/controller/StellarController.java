package com.telcobright.orchestrix.controller;

import com.telcobright.orchestrix.dto.stellar.QueryNode;
import com.telcobright.orchestrix.dto.stellar.EntityModificationRequest;
import com.telcobright.orchestrix.dto.stellar.MutationResponse;
import com.telcobright.orchestrix.service.StellarQueryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/stellar")
@CrossOrigin(origins = "*")
public class StellarController {

    @Autowired
    private StellarQueryService stellarQueryService;

    @PostMapping("/query")
    public ResponseEntity<Map<String, Object>> executeQuery(@RequestBody QueryNode query) {
        try {
            List<Map<String, Object>> result = stellarQueryService.executeQuery(query);
            
            // Wrap the result in a response format expected by the frontend
            Map<String, Object> response = new java.util.HashMap<>();
            response.put("success", true);
            response.put("data", result);
            response.put("count", result.size());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> errorResponse = new java.util.HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            errorResponse.put("data", new java.util.ArrayList<>());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    @PostMapping("/mutate")
    public ResponseEntity<MutationResponse> executeMutation(@RequestBody EntityModificationRequest request) {
        try {
            MutationResponse response = stellarQueryService.executeMutation(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            MutationResponse errorResponse = new MutationResponse();
            errorResponse.setSuccess(false);
            errorResponse.setMessage(e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
}