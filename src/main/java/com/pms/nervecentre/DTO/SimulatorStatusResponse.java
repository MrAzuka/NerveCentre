package com.pms.nervecentre.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SimulatorStatusResponse {
    private boolean running;
    private String message;
}
