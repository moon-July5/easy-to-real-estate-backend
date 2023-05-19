package com.example.finalproject.openapi.controller;

import com.example.finalproject.openapi.dto.ProxyReqDTO;
import com.example.finalproject.openapi.service.ProxyService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class ProxyController {
    private final ProxyService proxyService;

    @GetMapping("/api/proxy")
    public ResponseEntity<?> proxy(@RequestBody ProxyReqDTO proxyReqDTO) {
        RestTemplate restTemplate = new RestTemplate();
        HttpEntity<?> entity = new HttpEntity<>(new HttpHeaders());
        ResponseEntity<Map> resultMap = restTemplate.exchange(proxyService.makeUrl(proxyReqDTO),HttpMethod.GET, entity, Map.class);
        return resultMap;
    }
}
