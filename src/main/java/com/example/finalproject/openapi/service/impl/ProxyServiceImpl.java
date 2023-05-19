package com.example.finalproject.openapi.service.impl;

import com.example.finalproject.openapi.dto.ProxyReqDTO;
import com.example.finalproject.openapi.service.ProxyService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@Service
public class ProxyServiceImpl implements ProxyService {

    @Value("${api.serviceKey}")
    private String serviceKey;
    @Override
    public URI makeUrl(ProxyReqDTO proxyReqDTO) {
        System.out.println("proxyReqDTO = " + proxyReqDTO.getDealYmd());

        UriComponentsBuilder uribuilder = UriComponentsBuilder.newInstance()
                .scheme("http")
                .host("openapi.molit.go.kr")
                .path("OpenAPI_ToolInstallPackage/service/rest/RTMSOBJSvc/getRTMSDataSvcAptTradeDev")
                .queryParam("serviceKey", serviceKey)
                .queryParam("pageNo", proxyReqDTO.getPageNo())
                .queryParam("numOfRows", proxyReqDTO.getNumOfRows())
                .queryParam("LAWD_CD", proxyReqDTO.getLawdCd())
                .queryParam("DEAL_YMD", proxyReqDTO.getDealYmd())
                .queryParam("_type", "json");

        URI uri = URI.create(uribuilder.build().toUriString());

        return uri;
    }
}
