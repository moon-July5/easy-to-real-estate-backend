package com.example.finalproject.openapi.dto;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class ProxyReqDTO {
    private String pageNo;
    private String numOfRows;
    private String lawdCd;
    private String dealYmd;
}
