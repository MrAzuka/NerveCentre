package com.pms.nervecentre.Controller;


import com.pms.nervecentre.DTO.SimulatorStatusResponse;
import com.pms.nervecentre.Service.MetricSimulatorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/simulator")
@RequiredArgsConstructor
public class SimulatorController {

    private final MetricSimulatorService simulatorService;

    @GetMapping("/status")
    public ResponseEntity<SimulatorStatusResponse> status() {
        boolean running = simulatorService.isRunning();
        return ResponseEntity.ok(new SimulatorStatusResponse(
                running,
                running ? "Simulator is running" : "Simulator is stopped"
        ));
    }

    @PostMapping("/toggle")
    public ResponseEntity<SimulatorStatusResponse> toggle() {
        boolean newState = simulatorService.toggle();
        return ResponseEntity.ok(new SimulatorStatusResponse(
                newState,
                newState ? "Simulator started" : "Simulator stopped"
        ));
    }

    @PostMapping("/start")
    public ResponseEntity<SimulatorStatusResponse> start() {
        simulatorService.setRunning(true);
        return ResponseEntity.ok(new SimulatorStatusResponse(
                true, "Simulator started"
        ));
    }

    @PostMapping("/stop")
    public ResponseEntity<SimulatorStatusResponse> stop() {
        simulatorService.setRunning(false);
        return ResponseEntity.ok(new SimulatorStatusResponse(
                false, "Simulator stopped"
        ));
    }
}