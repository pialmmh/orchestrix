package com.telcobright.orchestrix.entity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import java.util.List;
import java.util.ArrayList;

@Converter
public class RolesConverter implements AttributeConverter<List<String>, String> {
    
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    @Override
    public String convertToDatabaseColumn(List<String> roles) {
        if (roles == null || roles.isEmpty()) {
            return "[]";
        }
        try {
            return objectMapper.writeValueAsString(roles);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error converting roles list to JSON", e);
        }
    }
    
    @Override
    public List<String> convertToEntityAttribute(String rolesJson) {
        if (rolesJson == null || rolesJson.isEmpty()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(rolesJson, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            throw new RuntimeException("Error converting JSON to roles list", e);
        }
    }
}