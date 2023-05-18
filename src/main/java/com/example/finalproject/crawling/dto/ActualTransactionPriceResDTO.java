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
public class ActualTransactionPriceResDTO {
    private String contract_date; // 계약 일자
    private String transaction_type; // 거래 유형
    private String price; // 거래 금액
    private String floor; // 건물 층수

    // 3년 단위로 추출
    public static List<ActualTransactionPriceResDTO> extractThreeYear(List<ActualTransactionPriceResDTO> input){
        List<ActualTransactionPriceResDTO> result = new ArrayList<>();

        LocalDate currentDate = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        for(ActualTransactionPriceResDTO dto : input){
            String dateString = dto.getContract_date();
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
