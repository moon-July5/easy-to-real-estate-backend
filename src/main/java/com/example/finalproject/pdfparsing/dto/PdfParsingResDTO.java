package com.example.finalproject.pdfparsing.dto;


import lombok.*;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString

public class PdfParsingResDTO {

    private HashMap<String, String> summary; // 매물요약

    private Map<Integer, HashMap<String, String>> ownership_list; // 등기부 요약 갑구

    private Map<Integer, HashMap<String, String>> withoutOwner; // 등기부 요약 갑구 이외

    private Map<Integer, HashMap<String, String>> rights_other_than_ownership; // 등기부 요약 을구

    private Long collateral_amount; // 채권최고액 합(담보총액)

    private int collateral_count; // 채권최고액 건수(담보건수)

    private List<LinkedHashMap<String, Long>> originalMoney; // 원금추론

//    private List<ActTransacAndMarketPriceResDTO> actTransacAndMarketPrice; // 실거래가 및 시세 정보
//
//    private List<ActualTransactionPriceResDTO> actualTransactionPrice; // 실거래가 정보
//
//    private List<MarketPriceResDTO> marketPrice; // 시세 정보

}
