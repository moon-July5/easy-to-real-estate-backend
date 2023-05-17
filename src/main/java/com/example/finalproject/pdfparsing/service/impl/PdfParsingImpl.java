package com.example.finalproject.pdfparsing.service.impl;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.example.finalproject.crawling.service.CrawlingService;
import com.example.finalproject.crawling.service.impl.CrawlingServiceImpl;
import com.example.finalproject.global.exception.KeywordValidationException;
import com.example.finalproject.global.exception.PDFValidationException;
import com.example.finalproject.global.response.CommonResponse;
import com.example.finalproject.global.response.ResponseService;
import com.example.finalproject.openapi.service.AddressCodeService;
import com.example.finalproject.pdfparsing.dto.PdfParsingResDTO;
import com.example.finalproject.pdfparsing.service.PdfParsingService;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdfparser.PDFParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.util.PDFTextStripper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@Service
@RequiredArgsConstructor
public class PdfParsingImpl implements PdfParsingService {

    private final AmazonS3 amazonS3Client;
    private final ResponseService responseService;
    private final CrawlingService crawlingService;
    private final AddressCodeService addressCodeService;

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
                    summaryParsing(pdfText, pdfParsingResDTO);
                    pdfText.contains("주요 등기사항 요약");
                }catch (Exception e){
                    throw new PDFValidationException();
                }
                withoutSummaryParsing(pdfText, pdfParsingResDTO);
                //originalMoneyParsing(pdfParsingResDTO);
                //craw(pdfParsingResDTO);
            } catch (Exception e) {
                throw new KeywordValidationException();
            }
            return responseService.getSingleResponse(pdfParsingResDTO);
        } else {
            return responseService.getFailResponse(404, "파일형식이 잘못되었습니다");
        }

    }

    // pdf주소가져오는 메서드;
    // address = 주소;
    public void craw(PdfParsingResDTO pdfParsingResDTO) throws Exception {
        HashMap<String, String> summary = pdfParsingResDTO.getSummary();
        String number = crawlingService.getComplexesNumber(summary.get("address"));
        if(!number.equals("")) {
            crawlingService.crawling(number, pdfParsingResDTO);
        } else {
            pdfParsingResDTO.setMarketPrice(null);
            pdfParsingResDTO.setActualTransactionPrice(null);
            pdfParsingResDTO.setActTransacAndMarketPrice(null);
            summary.put("lower_limit_price", null);
            summary.put("upper_limit_price", null);
            summary.put("actual_transaction_price", null);
            summary.put("units", null);
            summary.put("dong", null);
            summary.put("floors", null);
            summary.put("total_floors", null);
            summary.put("type", null);
        }
    }

    /**
     * withoutSummaryParsing
     * @param pdfText
     * @param pdfParsingResDTO
     */
    public void withoutSummaryParsing(String pdfText, PdfParsingResDTO pdfParsingResDTO) {

        String[] splitted = pdfText.split("주요 등기사항 요약", 2);
        String[] additional_split = splitted[splitted.length - 1].split("1[.]|2[.]|3[.]", 4);

        numberAddressFloorParsing(additional_split[0]);
        printingDateParsing(additional_split[3]);
        ownerParsing(additional_split[1], pdfParsingResDTO);
        withoutOwnerParsing(additional_split[2], pdfParsingResDTO);
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
     * 매물요약 파싱
     * @param pdfText
     * @param pdfParsingResDTO
     */
    public void summaryParsing(String pdfText, PdfParsingResDTO pdfParsingResDTO){

        String[] splitted = pdfText.split("주요 등기사항 요약", 2);
        String[] additional_split = splitted[splitted.length - 1].split("1[.]|2[.]|3[.]", 4);

        HashMap<String, String> summary = new HashMap<>();
        String viewedAt = printingDateParsing(additional_split[3]);
        String[] numberAddress = numberAddressFloorParsing(additional_split[0]);
        String area = exclusiveAreaParsing(pdfText);
        Double size = Double.parseDouble(area) * 0.3025;
        String pyeong = String.format("%.1f", size);
        String land_rights = landRightParsing(pdfText);
        String[] full_transfer_date = full_transfer_dateParsing(pdfText);

        summary.put("viewedAt", viewedAt);
        summary.put("address", numberAddress[1]);
        summary.put("newAddress", addressCodeService.findJibun(numberAddress[1]));
        summary.put("registryNumber", numberAddress[0]);
        summary.put("area", area);
        summary.put("pyeong", pyeong);
        summary.put("owner", "");
        summary.put("landRights", land_rights);
        summary.put("fullTransfer", full_transfer_date[0]);
        summary.put("ownerTransfer", full_transfer_date[1]);

        pdfParsingResDTO.setSummary(summary);
    }

    /**
     * 등기부 요약 - 갑구
     * @param pdfSplitParts
     * @param pdfParsingResDTO
     */
    public void ownerParsing(String pdfSplitParts, PdfParsingResDTO pdfParsingResDTO) {

        String regex = "(?<name>[가-힣]+) \\((소유자|공유자)\\) (?<age>\\d{6}-\\*{7}) (?<share>[0-9/분의|단독소유 ]+) (?<address>[^\\n\\r]+).* (?<rank>\\d+)";

        String[] splitted = pdfSplitParts.split("\n");

        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(pdfSplitParts);
        Map<Integer, HashMap<String, String>> ownerMap = new HashMap<>();
        int count = 0;
        while (matcher.find()) {
            HashMap<String, String> owner = new HashMap<>();
            StringBuilder sb = new StringBuilder();
            owner.put("name", matcher.group("name"));
            owner.put("age", matcher.group("age"));
            if (matcher.group("share").contains("분의")) {
                String[] shareArray = matcher.group("share").split("분의 ", 2);

                double share = Double.parseDouble(shareArray[shareArray.length - 1]) / Double.parseDouble(shareArray[0]);
                owner.put("share", String.valueOf(share));
                owner.put("percent", share * 100 + "%");
            } else {
                owner.put("share", "단독소유");
                owner.put("percent", "100%");
            }
            sb.append(matcher.group("address")).append(" ");
            sb.append(splitted[splitted.length - 1].trim());
            owner.put("address", String.valueOf(sb));
            owner.put("rank", matcher.group("rank"));

            ownerMap.put(count++, owner);
        }
        pdfParsingResDTO.setOwnership_list(ownerMap);
    }

    /**
     * 등기부 요약 - 갑구 이외
     * @param pdfSplitParts
     * @param pdfParsingResDTO
     */
    public void withoutOwnerParsing(String pdfSplitParts, PdfParsingResDTO pdfParsingResDTO){
        String regex = "(?<rank>\\d+-?\\d*(?:-\\d*)?) (?<purpose>[가-힣]+) .* (?<owner>[가-힣]+)";

        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(pdfSplitParts);
        Map<Integer, HashMap<String, String>> withoutOwnerMap = new HashMap<>();
        int count = 1;
        int i = 1;
        HashMap<Integer, String> acceptList = gapguAcceptParsing(pdfSplitParts);

        while (matcher.find()) {
            HashMap<String, String> ownerMap = new HashMap<>();
            ownerMap.put("rank", matcher.group("rank"));
            ownerMap.put("purpose", matcher.group("purpose"));
            ownerMap.put("owner", matcher.group("owner"));

            String accept = acceptList.get(i);
            ownerMap.put("accept", accept);

            HashMap<Integer, String> attachmentMoney = attachmentMoneyParsing(pdfSplitParts);
            String money = attachmentMoney.get(i);
            HashMap<Integer, String> attachmentName = attachmentNameParsing(pdfSplitParts);
            String name = attachmentName.get(i);
            StringBuilder sb = new StringBuilder();

            sb.append(money).append(" ").append(name);
            ownerMap.put("info", String.valueOf(sb));

            i++;
            withoutOwnerMap.put(count++, ownerMap);
        }
        pdfParsingResDTO.setWithoutOwner(withoutOwnerMap);
    }

    /**
     * 등기부 요약 - 을구
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
            rights_other_than_ownership.put("info", String.valueOf(sb));

            i++;
            max_mortgageBondMap.put(count++, rights_other_than_ownership);
        }
        pdfParsingResDTO.setRights_other_than_ownership(max_mortgageBondMap);
    }

    /**
     * 매물요약 - 매물일반주소지, 고유번호 파싱
     * @param pdfSplitParts
     * @return
     */
    public String[] numberAddressFloorParsing(String pdfSplitParts) {

        String[] splitted = pdfSplitParts.split("바랍니다.", 2);
        String[] additional_split = splitted[splitted.length - 1].split("\\[집합건물]|\\[건물]", 2);
        String[] match = new String[2];

        match[0] = additional_split[0].substring(6).trim(); // 고유번호
        match[1] = additional_split[additional_split.length - 1].trim(); // 지번주소
        return match;
    }

    /**
     * 매물요약 - 열람일시 파싱
     * @param pdfSplitParts
     * @return
     */
    public String printingDateParsing(String pdfSplitParts) {
        String[] splitted = pdfSplitParts.split("\n");
        String printTime = splitted[splitted.length - 1].substring(7).trim();
        DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("yyyy년 MM월 dd일 HH시 mm분 ss초");
        LocalDateTime dt = LocalDateTime.parse(printTime, inputFormatter);
        DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("yyyy년MM월dd일");
        return dt.format(outputFormatter);
    }

    /**
     * 매물요약 - 면적, 평수 파싱
     * @param pdfText
     * @return
     */
    public String exclusiveAreaParsing(String pdfText) {

        String[] splitted = pdfText.split("( 전유부분의 건물의 표시 )", 2);
        String[] additional_split = splitted[splitted.length - 1].split("( 대지권의 표시 )");
        String match = null;

        String regex = "\\d+\\.\\d+㎡";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(additional_split[0]);
        if (matcher.find()) {
            match = matcher.group(0).replace("㎡", "").trim();
            if(match.length() > 4){
                match = match.substring(0,5);
            }
        }
            return match;
    }

    /**
     * 매물요약 - 대지권 유무 파싱
     * @param pdfText
     * @return
     */
    public String landRightParsing(String pdfText){

        String[] splitted = pdfText.split("( 전유부분의 건물의 표시 )", 2);
        String[] additional_split = splitted[splitted.length - 1].split("( 대지권의 표시 )");

        for(String info : additional_split) {
            if (info.contains("소유권대지권")) {
                return "유";
            }
        }
        return "무";
    }

    /**
     * 매물요약 - 소유권이전, 지분전부이전 파싱
     * @param pdfText
     * @return
     */
    public String[] full_transfer_dateParsing(String pdfText) {

        String[] splitted = pdfText.split("【  을      구  】");
        String[] additional_split = splitted[0].split("( 소유권에 관한 사항 )");
        String[] lines = additional_split[additional_split.length - 1].split("\n");
        String regex = "(?<text>[가-힣]+지분전부) (?<date>\\d{4}년\\d{1,2}월\\d{1,2}일)";

        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(additional_split[additional_split.length - 1]);
        StringBuilder sb = new StringBuilder();
        StringBuilder sb2 = new StringBuilder();
        String[] match = new String[2];
        if (matcher.find()) {
            match[0] = matcher.group("date");
            match[0] = String.valueOf(sb.append("지분 전부이전 ").append(match[0]));
        }

        for (int i = lines.length-1; i >= 0; i--) {
            if(lines[i].contains("소유권이전")) {
                String[] words = lines[i].split(" ");
                match[1] = words[3];
                match[1] = String.valueOf(sb2.append("소유권이전 ").append(match[1]));
                break;
            }
        }
        return match;
    }


    /**
     * 요약 접수정보 파싱 - 갑구
     * @param pdfSplitParts
     * @return
     */
    public HashMap<Integer, String> gapguAcceptParsing(String pdfSplitParts){

        String[] splited = pdfSplitParts.split("3[.]");

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
                    acceptList.put(count++, String.valueOf(sb));
                }else{
                    sb.append(lines[i+4].replaceAll("[^(제\\d{1,10}호)]", ""));
                    acceptList.put(count++, String.valueOf(sb));
                }
                sb.setLength(0);
            }
        }
        return acceptList;
    }

    /**
     * 요약 청구금액 파싱 - 갑구
     * @param pdfSplitParts
     * @return
     */
    public HashMap<Integer, String> attachmentMoneyParsing(String pdfSplitParts) {

        String regex = "(청구금액)\\s+금(\\d{1,3}(,\\d{3})*) 원";
        Pattern pattern = Pattern.compile(regex);
        HashMap<Integer, String> attachment = new HashMap<>();
        String[] splitted = pdfSplitParts.split("\n");
        for (int i = 0; i < splitted.length - 2; i++) {
            Matcher matcher = pattern.matcher(splitted[i + 2]);

            if (matcher.find()) {
                String match = matcher.group();
                if (match.startsWith("청구금액")) {
                    attachment.put(i / 2 + 1, match);
                }
            }
        }
        return attachment;
    }

    /**
     * 요약 접수정보 파싱 - 을구
     * @param pdfSplitParts
     * @return
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
                    acceptList.put(count++, String.valueOf(sb));
                }else{
                    sb.append(lines[i+4].replaceAll("[^(제\\d{1,10}호)]", ""));
                    acceptList.put(count++, String.valueOf(sb));
                }
                sb.setLength(0);
            }
        }
        return acceptList;
    }

    /**
     * 요약 담보 총액 구하기 - 을구
     * @param pdfSplitParts
     * @param pdfParsingResDTO
     * @return
     */
    public HashMap<Integer, String> jeonseMortgageParsing(String pdfSplitParts, PdfParsingResDTO pdfParsingResDTO) {
        long sum_mortgageBond = 0; // 채권최고액 합
        int mortgageCount = 0; // 채권최고액 건수

        HashMap<Integer, String> max_mortgageBond = new HashMap<>();
        String[] splitted = pdfSplitParts.split("\n");

        String regex = "(채권최고액|전세금|채권액|임차보증금)\\s+금(\\d+,?)+원\\s";
        Pattern pattern = Pattern.compile(regex);
        for (int i = 0; i < splitted.length - 2; i++) {
           // if(i==25) {
                Matcher matcher = pattern.matcher(splitted[i + 2]);

                if (matcher.find()) {
                    String match = matcher.group();
                    long value = Long.parseLong(match.replaceAll("[^0-9]", ""));
                    if (match.startsWith("전세금")) {
                        max_mortgageBond.put(i / 2 + 1, match);
                    } else if (match.startsWith("채권최고액")) {
                        if (splitted[i + 2].contains("근저당권변경")) {
                            max_mortgageBond.put(i / 2 + 1, match);
                            sum_mortgageBond += value;
                            mortgageCount++;
                        }
                        max_mortgageBond.put(i / 2 + 1, match);
                        sum_mortgageBond += value;
                        mortgageCount++;
                    } else if (match.startsWith("채권액")) {
                        max_mortgageBond.put(i / 2 + 1, match);
                    } else if (match.startsWith("임차보증금")) {
                        max_mortgageBond.put(i / 2 + 1, match);
                    }
               // }
            }
        }
        pdfParsingResDTO.setCollateral_amount(sum_mortgageBond); // 채권최고액 합
        pdfParsingResDTO.setCollateral_count(mortgageCount); // 채권최고액 건수

        return max_mortgageBond;
    }

    /**
     * TODO 요약 주요등기사항 중 회사/사람 파싱 - 갑구&을구
     * @param pdfSplitParts
     * @return
     */
    public HashMap<Integer, String> attachmentNameParsing(String pdfSplitParts) {

        String regex = "(?<=(채권자|근저당권자|전세권자|임차권자)\\s{1,2})\\S+";
        Pattern pattern = Pattern.compile(regex);
        String[] lines = pdfSplitParts.split("\n");
        HashMap<Integer, String> attachmentName = new HashMap<>();
        for (int i = 0; i < lines.length - 2; i++) {
           // if(i==25) {
                Matcher matcher = pattern.matcher(lines[i + 2]);
                // 디버깅이 빡세면 if문으로 바로 가서 브레이킹포인트 잡고 해보기
                if (matcher.find()) {
                    //if(!lines[i+3].contains("제"))
                    attachmentName.put(i / 2 + 1, matcher.group());
                }
           // }
        }
        return attachmentName;
    }

    public void originalMoneyParsing(PdfParsingResDTO pdfParsingResDTO){
        LinkedHashMap<Long, LinkedMultiValueMap<String, Integer>> parse = new LinkedHashMap<>();
        LinkedMultiValueMap<String, Integer> value = new LinkedMultiValueMap<>();
        Map<Integer, HashMap<String, String>> original = pdfParsingResDTO.getRights_other_than_ownership();
        Long[] amount = new Long[original.size()];
        for (int i = 0; i < original.size(); i++) {
            HashMap<String, String> money = original.get(i+1);
            amount[i] = Long.parseLong(money.get("info").replaceAll("[^0-9]", ""));
            //if(money == null || money.equals(null))
                continue;
        } // 머니 없을떼 경우 continue;
        for (int i = 0; i < original.size(); i++) {
            value.add("110%", (int)(amount[i] / 1.1));
            value.add("115%", (int)(amount[i] / 1.15));
            value.add("120%", (int)(amount[i] / 1.2));
            value.add("130%", (int)(amount[i] / 1.3));
            value.add("140%", (int)(amount[i] / 1.4));
            value.add("150%", (int)(amount[i] / 1.5));
            // 값이 같기때문에 hashMap의 특성으로 리스폰 없을수도있음
            // key-value 형태의 hashMap 말고 찾아보기(순서대로인거)
            //멀티맵?

            parse.put(amount[i], value);
            //pdfParsingResDTO.setMemo(parse);
        }
        pdfParsingResDTO.setMemo(parse);
    }

}
