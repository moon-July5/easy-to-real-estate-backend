package com.example.finalproject.presubscribe.controller;

import com.example.finalproject.global.response.CommonResponse;
import com.example.finalproject.presubscribe.dto.PreSubscribeReqDTO;
import com.example.finalproject.presubscribe.service.PresubscribeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
public class PresubscribeController {
    private final PresubscribeService presubscribeService;

    @PostMapping("/presubscribe")
    public CommonResponse presubcribe(@RequestBody PreSubscribeReqDTO reqDTO){
        return presubscribeService.register(reqDTO);
    }
}
