package com.example.finalproject.crawling.dto;

import lombok.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

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

    // 3년 단위로 추출
    public static List<MarketPriceResDTO> extractThreeYear(List<MarketPriceResDTO> input){
        List<MarketPriceResDTO> result = new ArrayList<>();

        LocalDate currentDate = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        for(MarketPriceResDTO dto : input){
            String dateString = dto.getReference_date();
            // String을 LocalDate로 변환
            LocalDate date = LocalDate.parse(dateString, formatter);

            // 3년 단위로 비교하여 추출
            if (date.isAfter(currentDate.minusYears(1).minusDays(currentDate.getDayOfMonth()))
                    && date.isBefore(currentDate.plusMonths(1))){
                result.add(dto);
            }
        }
        return result;
    }
}
