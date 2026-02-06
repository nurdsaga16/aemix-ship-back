package com.example.aemix.dto.responses;

import lombok.Data;

@Data
public class InstructionLinkResponse {

    private Long id;
    private String linkKey;
    private String title;
    private String subtitle;
    private String link;
    private Integer sortOrder;
}
