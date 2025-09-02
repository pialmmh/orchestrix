package com.telcobright.orchestrix.entity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import java.util.List;
import java.util.ArrayList;

@Converter
public class TagsConverter implements AttributeConverter<List<String>, String> {
    
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    @Override
    public String convertToDatabaseColumn(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return "[]";
        }
        try {
            return objectMapper.writeValueAsString(tags);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error converting tags list to JSON", e);
        }
    }
    
    @Override
    public List<String> convertToEntityAttribute(String tagsJson) {
        if (tagsJson == null || tagsJson.isEmpty()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(tagsJson, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            throw new RuntimeException("Error converting JSON to tags list", e);
        }
    }
}