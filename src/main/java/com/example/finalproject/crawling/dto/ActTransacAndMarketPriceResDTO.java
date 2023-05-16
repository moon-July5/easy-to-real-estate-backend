package com.example.finalproject.crawling.dto;

import lombok.*;

// 실거래가 및 시세 정보(네이버 부동산)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class ActTransacAndMarketPriceResDTO {
    private String transaction_type; // 거래 유형
    private String lower_limit_price; // 하한가
    private String upper_limit_price; // 상한가
    private String sales_vs_rent_price; // 매매가 대비 전세가
    private String recent_trades; // 최근 매매 실거래가
}
