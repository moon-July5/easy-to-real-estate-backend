package com.example.finalproject.pdfparsing.service.impl;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.example.finalproject.global.exception.KeywordValidationException;
import com.example.finalproject.global.exception.PDFValidationException;
import com.example.finalproject.global.response.CommonResponse;
import com.example.finalproject.global.response.ResponseService;
import com.example.finalproject.pdfparsing.dto.PdfParsingResDTO;
import com.example.finalproject.pdfparsing.service.PdfParsingService;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdfparser.PDFParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.util.PDFTextStripper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@Service
@RequiredArgsConstructor
public class PdfParsingImpl implements PdfParsingService {

    private final AmazonS3 amazonS3Client;
    private final ResponseService responseService;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    @Override
    public CommonResponse pdfParsing(MultipartFile multipartFile) throws IOException, PDFValidationException {

        String fileExtension = "";
        try {
            String fileName = multipartFile.getOriginalFilename();
            fileExtension = Objects.requireNonNull(fileName).substring(fileName.lastIndexOf(".") + 1);
        } catch (NullPointerException e) {
            return responseService.getFailResponse(400, "파일이 없거나 잘못된 접근입니다.");
        }

        if (fileExtension.equals("pdf")) {
            String key = uploadFileToS3(multipartFile);
            S3Object s3Object = amazonS3Client.getObject(bucket, key);
            InputStream inputStream = s3Object.getObjectContent();


            PDFParser pdfParser = new PDFParser(inputStream);
            pdfParser.parse();
            PDDocument document = pdfParser.getPDDocument();
            PDFTextStripper pdfStripper = new PDFTextStripper();
            String pdfText = pdfStripper.getText(document);
            document.close();

            PdfParsingResDTO pdfParsingResDTO = new PdfParsingResDTO();

            try {
                try {
                    //exclusiveAreaParsing(pdfText, pdfParsingResDTO);
                    //titleLandRightParsing(pdfText, pdfParsingResDTO);
                    pdfText.contains("주요 등기사항 요약");
                }catch (Exception e){
                    throw new PDFValidationException();
                }
                summaryParsing(pdfText, pdfParsingResDTO);
            } catch (Exception e) {
                throw new KeywordValidationException();
            }
            return responseService.getSingleResponse(pdfParsingResDTO);
        } else {
            return responseService.getFailResponse(404, "파일형식이 잘못되었습니다");
        }

    }

    /**
     * summary Parsing
     * @param pdfText
     * @param pdfParsingResDTO
     */
    public void summaryParsing(String pdfText, PdfParsingResDTO pdfParsingResDTO) {

        String[] splitted = pdfText.split("주요 등기사항 요약", 2);
        String[] additional_split = splitted[splitted.length - 1].split("1[.]|2[.]|3[.]", 4);

        rights_other_than_ownershipParsing(additional_split[3], pdfParsingResDTO);
    }

    /**
     * S3업로드
     * @param multipartFile
     * @return
     */
    public String uploadFileToS3(MultipartFile multipartFile) {
        String key = multipartFile.getOriginalFilename();
        InputStream inputStream;
        try {
            inputStream = multipartFile.getInputStream();
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(multipartFile.getSize());
            amazonS3Client.putObject(new PutObjectRequest(bucket, key, inputStream, metadata));
        } catch (IOException e) {
            throw new PDFValidationException();
        }
        return key;
    }

    /**
     * 을구 소유권 이외 어쩌구 파싱
     * @param pdfSplitParts
     * @param pdfParsingResDTO
     */
    public void rights_other_than_ownershipParsing(String pdfSplitParts, PdfParsingResDTO pdfParsingResDTO){

        String regex = "(?<rank>\\d+-?\\d*(?:-\\d*)?) (?<purpose>[가-힣]+) .* (?<owner>[가-힣]+)";

        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(pdfSplitParts);
        Map<Integer, HashMap<String, String>> max_mortgageBondMap = new HashMap<>();
        int count = 1;
        int i = 1;
        HashMap<Integer, String> acceptList = acceptParsing(pdfSplitParts);

        if(matcher.find() == false) {
            pdfParsingResDTO.setRights_other_than_ownership(null);
        }else {
            while (matcher.find()) {
                HashMap<String, String> rights_other_than_ownership = new HashMap<>();
                rights_other_than_ownership.put("rank", matcher.group("rank"));
                rights_other_than_ownership.put("purpose", matcher.group("purpose"));
                rights_other_than_ownership.put("owner", matcher.group("owner"));

                String accept = acceptList.get(i);
                rights_other_than_ownership.put("accept", accept);

                HashMap<Integer, String> max_mortgageBond = jeonseMortgageParsing(pdfSplitParts, pdfParsingResDTO);
                String max = max_mortgageBond.get(i);
                HashMap<Integer, String> attachmentName = attachmentNameParsing(pdfSplitParts);
                String name = attachmentName.get(i);
                StringBuilder sb = new StringBuilder();

                sb.append(max).append(" ").append(name);
                rights_other_than_ownership.put("info", sb.toString());

                i++;
                max_mortgageBondMap.put(count++, rights_other_than_ownership);
            }
            pdfParsingResDTO.setRights_other_than_ownership(max_mortgageBondMap);
        }

    }

    /**
     * 접수정보 파싱
     * @param pdfSplitParts
     */
    public HashMap<Integer, String> acceptParsing(String pdfSplitParts){

        String[] splited = pdfSplitParts.split("가.");

        String regex = "\\d{4}년\\d{1,2}월\\d{1,2}일";
        Pattern pattern = Pattern.compile(regex);
        String[] lines = splited[0].split("\n");
        HashMap<Integer, String> acceptList = new HashMap<>();
        Matcher matcher;
        int count = 1;
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < lines.length; i++) {
            matcher = pattern.matcher(lines[i]);
            if(matcher.find()){
                sb.append(matcher.group()).append(" ");
                if(lines[i+1].contains("제")){
                    sb.append(lines[i+1].replaceAll("[^(제\\d{1,10}호)]", ""));
                    acceptList.put(count++, sb.toString());
                }else{
                    sb.append(lines[i+4].replaceAll("[^(제\\d{1,10}호)]", ""));
                    acceptList.put(count++, sb.toString());
                }
                sb.setLength(0);
            }
        }
        return acceptList;
    }

    /**
     * 담보 총액 구하기
     * @param pdfSplitParts
     * @param pdfParsingResDTO
     */
    public HashMap<Integer, String> jeonseMortgageParsing(String pdfSplitParts, PdfParsingResDTO pdfParsingResDTO) {
        long sumJeonse_deposit = 0; // 전세금 합
        int jeonseCount = 0; // 전세금 건수
        long sum_mortgageBond = 0; // 채권최고액 합
        int mortgageCount = 0; // 채권최고액 건수

        HashMap<Integer, String> max_mortgageBond = new HashMap<>();
        String[] splitted = pdfSplitParts.split("\n");

        if (pdfSplitParts.contains("근저당권변경")) {
            long previousAmount = 0;

            for (String line : splitted) {
                if (line.contains("채권최고액")) {
                    String[] words = line.split(" ");
                    long amount = Long.parseLong(words[5].replaceAll("[^0-9]", ""));
                    if (line.contains("근저당권변경")) {
                        sum_mortgageBond -= previousAmount;
                        mortgageCount--;
                    }
                    sum_mortgageBond += amount;
                    mortgageCount++;
                    previousAmount = amount;
                }
            }
            pdfParsingResDTO.setCollateral_count(mortgageCount);
            return max_mortgageBond;

        } else {
            String regex = "(채권최고액|전세금|채권액|임차보증금)\\s+금(\\d+,?)+원\\s";
            Pattern pattern = Pattern.compile(regex);
            for (int i = 0; i < splitted.length - 2; i++) {

                Matcher matcher = pattern.matcher(splitted[i + 2]);

                if (matcher.find()) {
                    String match = matcher.group();
                    long value = Long.parseLong(match.replaceAll("[^0-9]", ""));
                    if (match.startsWith("전세금")) {
                        max_mortgageBond.put(i/2 + 1, match);
                        sumJeonse_deposit += value;
                    } else if (match.startsWith("채권최고액")) {
                        max_mortgageBond.put(i/2 + 1, match);
                        sum_mortgageBond += value;
                        mortgageCount++;
                    } else if (match.startsWith("채권액")) {
                        max_mortgageBond.put(i/2 + 1, match);
                    } else if (match.startsWith("임차보증금")) {
                        max_mortgageBond.put(i/2 + 1, match);
                    }
                }
            }

            pdfParsingResDTO.setJeonse_amount(sumJeonse_deposit); // 전세금 합
            pdfParsingResDTO.setJeonse_count(jeonseCount); // 전세금 건수
            pdfParsingResDTO.setCollateral_amount(sum_mortgageBond); // 채권최고액 합
            pdfParsingResDTO.setCollateral_count(mortgageCount); // 채권최고액 건수
        }
        return max_mortgageBond;
    }

    /**
     * 주요등기사항 중 회사/사람 파싱
     * @param pdfSplitParts
     */
    public HashMap<Integer, String> attachmentNameParsing(String pdfSplitParts) {

        String regex = "(?<=(채권자|근저당권자|전세권자|임차권자)\\s{1,2})\\S+";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(pdfSplitParts);
        HashMap<Integer, String> attachmentName = new HashMap<>();
        int count = 1;
        while (matcher.find()) {
            attachmentName.put(count, matcher.group());
            count++;
        }
        return attachmentName;
    }

}
