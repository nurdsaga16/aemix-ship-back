package com.example.aemix.dto.responses;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UploadOrdersResponse {
    private int total;
    private int created;
    private int skipped;
    private List<String> errors;
}
