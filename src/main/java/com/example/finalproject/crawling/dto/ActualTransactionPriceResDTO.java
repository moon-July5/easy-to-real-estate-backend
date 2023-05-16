package com.example.finalproject.crawling.dto;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class ActualTransactionPriceResDTO {
    private String contract_date; // 계약 일자
    private String transaction_type; // 거래 유형
    private String price; // 거래 금액
    private String floor; // 건물 층수
    //private String asking_price; // 해당 매물의 요구 매매가격 또는 임대료;
}
