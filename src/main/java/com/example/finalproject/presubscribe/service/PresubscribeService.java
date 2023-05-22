package com.example.finalproject.presubscribe.service;


import com.example.finalproject.global.response.CommonResponse;
import com.example.finalproject.presubscribe.dto.PreSubscribeReqDTO;

public interface PresubscribeService {
    public CommonResponse register(PreSubscribeReqDTO reqDTO);
}
