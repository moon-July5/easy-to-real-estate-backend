package com.example.finalproject.crawling.dto;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class MarketPriceResDTO {
    private String reference_date; // 기준일
    private String transaction_type; // 거래 유형 (매매, 전세)
    private String lower_limit_price; // 하한가
    private String upper_limit_price; // 상한가
    private String average_change; // 평균변동액
    private String sales_vs_rent_price; // 매매가 대비 전세가
}
