package org.crochet.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/")
public class RootController {

    @RequestMapping(method = { RequestMethod.GET, RequestMethod.HEAD })
    public ResponseEntity<Map<String, Object>> rootEndpoint() {
        Map<String, Object> response = new HashMap<>();
        response.put("name", "Crochet Web API");
        response.put("version", "1.0.0");
        response.put("status", "running");
        response.put("message", "Welcome to Crochet Web API");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> healthCheck() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        return ResponseEntity.ok(response);
    }
}