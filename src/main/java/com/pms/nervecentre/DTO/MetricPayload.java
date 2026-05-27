package com.pms.nervecentre.DTO;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.Map;

@Data
public class MetricPayload {
    @NotBlank
    private String name;
    @NotBlank
    private String value;

    private Map<String, String> tags;
}
