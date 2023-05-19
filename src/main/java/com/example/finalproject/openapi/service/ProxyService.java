package com.example.finalproject.openapi.service;

import com.example.finalproject.openapi.dto.ProxyReqDTO;

import java.net.URI;

public interface ProxyService {

    public URI makeUrl(ProxyReqDTO proxyReqDTO);
}
