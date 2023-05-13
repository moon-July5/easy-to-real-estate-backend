package com.example.finalproject.pdfparsing.dto;



import lombok.*;

import java.util.HashMap;
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

    private String viewed_at; // 등기부등본 열람일시

    private String registry_number; // 등기부등본 고유번호

    private String address; // 매물의 일반주소지


//    private String uniqueNumber; // 고유번호
//
//    private String maxFloor; //최고층수
//
//    private String currentFloor; // 현재 층

//    private String address; // 주소
//
//    private double landRightRatio; // 대지권비율
//
//    private Map<Integer, HashMap<String, String>> owner; // 소유자
//
//    private String exclusiveArea; //전용면적
//
//    private Long sumJeonse_deposit; // 보증금
//
//    private Map<Integer, String> jeonseAuthorityList; // 전세권자 리스트
//
//    private int mortgageCount; // 근저당 건수
//
//    private Map<Integer, String> mortgageeList; // 근저당권자 리스트
//
//    private Long sumMax_mortgageBond; // 채권최고액의 합
//
//    private int pledgeCount; // 질권설정 건수
//
//    private Map<Integer, String> pledgeCreditorList; // 질권 채권자 리스트
//
//    private Long sumPledge; // 질권설정 채권액의 합
//
//    private int attachmentCount; // 압류건수
//
//    private Long sumAncillary_Attachment; // 가압류 청구금액의 합
//
//    private Map<Integer, String> attachmentList; // 압류 채권자 리스트
//
//    private String printingDate; // 열람일시

}
