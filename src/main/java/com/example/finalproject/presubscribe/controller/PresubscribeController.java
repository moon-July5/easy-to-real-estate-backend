package com.example.finalproject.presubscribe.controller;

import com.example.finalproject.global.response.CommonResponse;
import com.example.finalproject.presubscribe.service.PresubscribeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
public class PresubscribeController {
    private final PresubscribeService presubscribeService;

    @PostMapping("/presubscribe")
    public CommonResponse presubcribe(@RequestParam(name = "email") String email){
        return presubscribeService.register(email);
    }
}
