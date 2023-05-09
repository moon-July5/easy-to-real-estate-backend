package com.example.finalproject.openapi.service;

import com.example.finalproject.global.response.CommonResponse;

public interface AddressCodeService {

    public CommonResponse regCodeApi() throws Exception;

    public String findAddressCode(String address);
    public String findAptName(String address);

}
