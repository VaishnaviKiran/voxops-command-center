package com.voxops.voice;

import com.voxops.common.ServiceHealthResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/voice")
public class VoiceController {

    @GetMapping("/health")
    public ServiceHealthResponse health() {
        return ServiceHealthResponse.up("voice-stream-service");
    }
}
