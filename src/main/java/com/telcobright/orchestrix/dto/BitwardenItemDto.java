package com.telcobright.orchestrix.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BitwardenItemDto {
    private String id;
    private String organizationId;
    private String folderId;
    private String name;
    private String notes;
    private List<String> collectionIds;
    private String revisionDate;
    private Boolean favorite;
    private Integer type; // 1 = Login, 2 = Secure Note, 3 = Card, 4 = Identity
}