package com.example.finalproject.crawling.controller;

import com.example.finalproject.crawling.service.CrawlingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
public class CrawlingController {
    private final CrawlingService crawlingService;

}
