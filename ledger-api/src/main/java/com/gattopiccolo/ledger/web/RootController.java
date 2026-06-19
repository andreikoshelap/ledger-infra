package com.gattopiccolo.ledger.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class RootController {

    @GetMapping("/")
    public Map<String, String> index() {
        return Map.of(
            "status", "UP",
            "message", "Welcome to Ledger API",
            "version", "4.1.0"
        );
    }
}
