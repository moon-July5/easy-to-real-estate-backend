package com.example.finalproject.crawling.service.impl;

import com.example.finalproject.crawling.dto.ActTransacAndMarketPriceResDTO;
import com.example.finalproject.crawling.dto.ActualTransactionPriceResDTO;
import com.example.finalproject.crawling.dto.MarketPriceResDTO;
import com.example.finalproject.crawling.service.CrawlingService;
import com.example.finalproject.global.response.ResponseService;
import com.example.finalproject.openapi.service.AddressCodeService;
import com.example.finalproject.pdfparsing.dto.PdfParsingResDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Log4j2
@RequiredArgsConstructor
@Service
public class CrawlingServiceImpl implements CrawlingService {
    private final AddressCodeService addressCodeService;

    @Override
    public void crawling(String complexesNumber, PdfParsingResDTO pdfParsingResDTO) {
        ChromeDriver driver = option();

        List<ActualTransactionPriceResDTO> actualTransactionPriceList = new ArrayList<>();
        List<MarketPriceResDTO> marketPriceList = new ArrayList<>();
        List<ActTransacAndMarketPriceResDTO> actTransacAndMarketPriceList = new ArrayList<>();

        HashMap<String, String> summary = pdfParsingResDTO.getSummary();
        String area = summary.get("area"); // 전용 면적
        String address = summary.get("address");

        // 스크립트를 사용하기 위한 캐스팅
        JavascriptExecutor js = (JavascriptExecutor) driver;

        String url = "https://new.land.naver.com/complexes/"+complexesNumber;

        // 크롬 열기
        driver.get(url);

        // 현재 페이지의 소스 코드 가져오기
        Document document = Jsoup.parse(driver.getPageSource());

        // 드라이버가 실행된 뒤 최대 2초까지 기다림
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(2));

        try{
            // 단지 정보 클릭
            driver.findElement(By.xpath("//*[@id=\"summaryInfo\"]/div[2]/div[2]/button[1]")).click();
        } catch (Exception e){
            e.printStackTrace();
        }

        // id 선택자가 선택한 부분이 존재할 때까지 기다림(단지 내 면적별 정보)
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("tab0"))).click();

        // 현재 페이지의 소스코드 가져오기(페이지 소스 업데이트)
        document = Jsoup.parse(driver.getPageSource());

        String complexType = document.selectXpath("//*[@id=\"summaryInfo\"]/div[1]/span").text();
        String floor = document.selectXpath("//*[@id=\"detailContents1\"]/div[1]/table/tbody/tr[1]/td[2]").text();

        Elements complexInfo = document.select("div.list_contents div.list_fixed div.list_complex_info dl.complex_feature dd");
        String units = complexInfo.get(0).text();
        String dong = complexInfo.get(1).text();

        Elements widthInfo = document.select("div.detail_box--floor_plan span.detail_sorting_width a.detail_sorting_tab");

        // 더보기 탭 클릭
        try{
            driver.findElement(By.className("btn_moretab")).click();
        }catch(Exception e){
//           e.printStackTrace();
        }

        // 해당 매물의 면적 수 만큼
        for (int num = 0; num < widthInfo.size(); num++) {
            // 면적 click 을 위한 xpath 설정
            String xpath = String.format("//*[@id=\"tab%d\"]", num);
            // tab을 클릭함
            driver.findElement(By.xpath(xpath)).click();

            // 탭 클릭때마다 0.1초 쉼
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // 현재 페이지의 소스코드 가져오기(페이지 소스 업데이트)
            document = Jsoup.parse(driver.getPageSource());

            // 공급/전용, 방수/욕실수, 해당면적 세대수.. 등 에 대한 정보 table
            Elements sizeInfosTable = document.select("div.detail_box--floor_plan table.info_table_wrap tr.info_table_item");

            String size = null;

            for (Element detailsTable : sizeInfosTable) {
                // 여기서 오류가 발생할 확률이 있으므로 예외처리로 오류가 발생하면 정보를 못가져오게
                // 매물마다 있는 정보가 있고 없는 정보가 있기 때문에 오류가 발생할 수 있음
                try {
                    // 공급&전용이 첫 번째이기 때문에 면적만 추출 후 berak
                    if (detailsTable.select("th.table_th").text().equals("공급/전용")) {
                        size = detailsTable.select("td.table_td").text();
                        break;
                    }
                } catch (IllegalStateException e) {
                    e.printStackTrace();
                }
            }

            // 등기부 등본에 존재하는 전용 면적과 크롤링한 전용 면적 값 비교
            if(getArea(size).equals(area))
                break;
        }

        // 시세/실거래가 클릭
        driver.findElement(By.xpath("//*[@id=\"summaryInfo\"]/div[2]/div[2]/button[2]")).click();

        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//*[@id=\"summaryInfo\"]/div[2]/div[2]/button[2]"))).click();
        // 현재 페이지의 소스코드 가져오기(페이지 소스 업데이트)
        document = Jsoup.parse(driver.getPageSource());

        // "매매", "전세"
        Elements sellingType = document.select("div.detail_box--chart div.detail_sorting_tabs--underbar a");

        for(int i=0; i<sellingType.size()-1; i++){
            String id = String.format("marketPriceTab%d", i+1);

            driver.findElement(By.id(id)).sendKeys(Keys.ENTER);
            wait.until(ExpectedConditions.presenceOfElementLocated(By.id(id))).click();

            // 현재 페이지의 소스코드 가져오기(페이지 소스 업데이트)
            document = Jsoup.parse(driver.getPageSource());

            String type = sellingType.get(i).text();

            // 매매/전세 실거래가 부분
            int isActualPrice = document.select("div.detail_price_data table.type_real").size();

            if(isActualPrice!=0) {
                //int displayOk = driver.findElement(By.cssSelector("div.detail_price_data button.detail_data_more")).isDisplayed();
                int isButton = document.selectXpath("//*[@id=\"tabpanel1\"]/div[5]/button").size();

                int count = 0;
                // 시세 더보기 버튼이 존재하면 클릭
                while (isButton!=0) {
                    try {
                        driver.findElement(By.cssSelector("div.detail_price_data button.detail_data_more")).click();
                        //wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("div.detail_price_data button.detail_data_more"))).click();

                        // 탭 클릭때마다 0.1초 쉼
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                        count++;

                        // 현재 페이지의 소스코드 가져오기(페이지 소스 업데이트)
                        document = Jsoup.parse(driver.getPageSource());

                        //displayOk = driver.findElement(By.cssSelector("div.detail_price_data button.detail_data_more")).isDisplayed();
                        isButton = document.selectXpath("//*[@id=\"tabpanel1\"]/div[5]/button").size();

                        if (count > 10)
                            break;
                    } catch (NoSuchElementException ne) {
                        break;
                    }
                }

                Elements actualPrice = document.select("div.detail_price_data table.type_real tbody tr");

                for (int j = 0; j < actualPrice.size(); j++) {
                    Element row = actualPrice.get(j);

                    // 계약월
                    String contractDate = row.select("th").text().replaceAll("\\.", "-");

                    // 매매가 정보들(계약월에 따라 정보가 여러 개 존재할 수 있음)
                    Elements priceInfos = row.select("td div.detail_table_info div.detail_info_inner span.detail_info_item");

                    for (int k = 0; k < priceInfos.size(); k++) {
                        ActualTransactionPriceResDTO actualTransactionPrice = new ActualTransactionPriceResDTO();

                        String info = priceInfos.get(k).text();
                        String[] dayAndFloor = getDayAndFloor(info);

                        actualTransactionPrice.setTransaction_type(type);
                        actualTransactionPrice.setPrice(getPrice(info));
                        actualTransactionPrice.setContract_date(contractDate + dayAndFloor[0]);
                        actualTransactionPrice.setFloor(dayAndFloor[1]);

                        actualTransactionPriceList.add(actualTransactionPrice);
                    }
                }
            } else {
                pdfParsingResDTO.setActualTransactionPrice(null);
            }

            // 매매/전세 시세 부분
            int isMarketPrice = document.select("div.detail_price_data table.type_price").size();
            String tabName = document.selectXpath("//*[@id=\"tabpanel1\"]/div[6]/div/button[1]").text();

            if(isMarketPrice!=0) {
                //boolean displayOk = driver.findElement(By.xpath("//*[@id=\"tabpanel1\"]/div[7]/button")).isDisplayed();
                int isButton = document.selectXpath("//*[@id=\"tabpanel1\"]/div[7]/button").size();

                int count = 0;
                // 시세 더보기 버튼이 존재하면 클릭
                while (isButton!=0) {
                    try {
                        driver.findElement(By.xpath("//*[@id=\"tabpanel1\"]/div[7]/button")).click();
                        //wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("div.detail_price_data button.detail_data_more"))).click();

                        // 탭 클릭때마다 0.1초 쉼
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                        count++;

                        //displayOk = driver.findElement(By.xpath("//*[@id=\"tabpanel1\"]/div[7]/button")).isDisplayed();
                        isButton = document.selectXpath("//*[@id=\"tabpanel1\"]/div[7]/button").size();

                        // 현재 페이지의 소스코드 가져오기(페이지 소스 업데이트)
                        document = Jsoup.parse(driver.getPageSource());

                        if (count > 13)
                            break;
                    } catch (NoSuchElementException ne) {
                        break;
                    }
                }

                Elements marketPrice = document.select("div.detail_price_data table.type_price tbody tr");

                for (int j = 0; j < marketPrice.size(); j++) {
                    MarketPriceResDTO marketPriceResDTO = new MarketPriceResDTO();

                    Element row = marketPrice.get(j);

                    String referenceDate = row.select("th").text().replaceAll("\\.", "-").substring(0, 10);
                    String lowerLimitPrice = row.select("td").get(0).text();
                    String upperLimitPrice = null;

                    if(tabName.equals("한국부동산원")) {
                        upperLimitPrice = row.select("td").get(1).text();
                    } else {
                        upperLimitPrice = row.select("td").get(2).text();
                    }

                    String averageChange = null;
                    if(tabName.equals("한국부동산원")) {
                        if (row.select("td").get(2).text().equals("-")) {
                            averageChange = row.select("td").get(2).text();
                        } else {
                            averageChange = row.select("td strong.detail_table_price span.detail_price_text").text();
                        }
                    } else {
                        averageChange = "-";
                    }

                    String salesVsRentPrice = row.select("td").get(3).text();

                    marketPriceResDTO.setReference_date(referenceDate);
                    marketPriceResDTO.setTransaction_type(type);
                    marketPriceResDTO.setLower_limit_price(lowerLimitPrice);
                    marketPriceResDTO.setUpper_limit_price(upperLimitPrice);
                    marketPriceResDTO.setAverage_change(averageChange);
                    marketPriceResDTO.setSales_vs_rent_price(salesVsRentPrice);

                    marketPriceList.add(marketPriceResDTO);
                }
            } else {
                pdfParsingResDTO.setMarketPrice(null);
            }

            ActTransacAndMarketPriceResDTO actTransacAndMarketPriceResDTO = new ActTransacAndMarketPriceResDTO();

            // 최근 매매 실거래가 있는 경우
            if(document.select("dl.complex_price--trade").size() != 0){
                Elements recent = document.select("dl.complex_price--trade");

                String recentTrades = recent.select("dd").get(0).text();

                actTransacAndMarketPriceResDTO.setRecent_trades(recentTrades);
            } else {
                actTransacAndMarketPriceResDTO.setRecent_trades("-");
            }

            // 매매/전세 하한가 및 상한가, 매매가 대비 전세가 추출
            // 값이 있는 경우
            if (document.select("div.detail_asking_price").size() != 0) {
                // 상한가, 하한가의 정보가 들은 테이블 소스
                Elements minMaxTable = document.select("div.detail_table_cell");

                // 하한가 가격
                String minPrice = minMaxTable.get(0).select("strong").text();

                // 상한가 가격
                String maxPrice = minMaxTable.get(1).select("strong").text();

                // 매매가 대비 전세가 퍼센트(%)
                String rentFeePrice = minMaxTable.get(2).select("strong").text();

                actTransacAndMarketPriceResDTO.setLower_limit_price(minPrice);
                actTransacAndMarketPriceResDTO.setUpper_limit_price(maxPrice);
                actTransacAndMarketPriceResDTO.setSales_vs_rent_price(rentFeePrice);
            } else {
                actTransacAndMarketPriceResDTO.setLower_limit_price("-");
                actTransacAndMarketPriceResDTO.setUpper_limit_price("-");
                actTransacAndMarketPriceResDTO.setSales_vs_rent_price("-");
            }

            actTransacAndMarketPriceResDTO.setTransaction_type(type);
            actTransacAndMarketPriceList.add(actTransacAndMarketPriceResDTO);
        }

        driver.quit();

        pdfParsingResDTO.setActTransacAndMarketPrice(actTransacAndMarketPriceList);
        pdfParsingResDTO.setActualTransactionPrice(actualTransactionPriceList);
        pdfParsingResDTO.setMarketPrice(marketPriceList);
        summary.put("lower_limit_price", actTransacAndMarketPriceList.get(0).getLower_limit_price());
        summary.put("upper_limit_price", actTransacAndMarketPriceList.get(0).getUpper_limit_price());
        summary.put("actual_transaction_price", actualTransactionPriceList.get(0).getPrice() + "("+actualTransactionPriceList.get(0).getContract_date()+", "+actualTransactionPriceList.get(0).getFloor()+"층)");
        summary.put("units", units);
        summary.put("dong", dong);
        summary.put("floors", getCurrentFloor(address));
        summary.put("total_floors", getFloor(floor));
        summary.put("type", complexType);
    }

    // 매물의 고유 번호 추출
    @Override
    public String getComplexesNumber(String address) throws Exception {
        String addressCode = addressCodeService.findAddressCode(address);
        String aptName = addressCodeService.findAptName(address);

        ChromeDriver driver = option();
        String url = "https://new.land.naver.com/api/regions/complexes?cortarNo="+addressCode+"&realEstateType=APT:PRE&order=";

        driver.get(url);

        Document document = Jsoup.parse(driver.getPageSource());

        String preview = driver.findElement(By.tagName("pre")).getText();

        driver.quit();

        JSONParser parser = new JSONParser();
        JSONObject object = (JSONObject) parser.parse(preview);
        JSONArray array = (JSONArray) object.get("complexList");

        String number = "";

        for (int i = 0; i < array.size(); i++) {
            object = (JSONObject) array.get(i);

            String complexName = object.get("complexName").toString().replaceAll("\\(.*\\)", "");

            if (aptName.contains(complexName)){
                number = object.get("complexNo").toString();
                break;
            }
        }

        return number;
    }

    // 최고층만 추출
    private String getFloor(String input){
        Pattern pattern = Pattern.compile("\\d+층/(\\d+층)"); // 정규표현식 패턴 설정
        Matcher matcher = pattern.matcher(input);

        String result = null;

        if (matcher.find()) {
            result = matcher.group(1);
        }

        return result;
    }

    // 현재 매물의 층만 추출
    private String getCurrentFloor(String input) {
        Pattern pattern = Pattern.compile("제(\\d+)층");
        Matcher matcher = pattern.matcher(input);

        String floor = "";

        if (matcher.find()) {
            floor = matcher.group(1);
        }

        return floor;
    }
    // 전용 면적 값만 추출하기 위한 메서드
    private String getArea(String input){
        // 소수점 포함된 숫자를 추출하는 정규표현식
        Pattern pattern = Pattern.compile("\\d+(\\.\\d+)?");
        Matcher matcher = pattern.matcher(input);

        String target = null;

        if (matcher.find()) {
            // 첫 번째로 매칭된 결과인 공급 면적을 가져옴
            String result = matcher.group();
            // 두 번째로 매칭된 결과인 전용 면적을 가져오기 위해 find() 메서드를 한 번 더 호출함
            matcher.find();
            // 두 번째로 매칭된 결과인 "84.99"를 가져옴
            target = matcher.group();
        }

        return target;
    }

    // 매매 실거래가에서 매매가만 추출
    private String getPrice(String input){
        Pattern pattern = Pattern.compile("^(.*?)\\(");
        Matcher matcher = pattern.matcher(input);

        String result = null;

        if (matcher.find()) {
            result = matcher.group(1);
        }

        return result;
    }

    // 매매 실거래가에서 일과 층 추출
    private String[] getDayAndFloor(String input){
        Pattern pattern2 = Pattern.compile("(\\d+)일,(\\d+)층");
        Matcher matcher2 = pattern2.matcher(input);

        String[] result = new String[2];

        if (matcher2.find()) {
            // 일
            String result1 = matcher2.group(1);
            // 층
            String result2 = matcher2.group(2);
            result[0] = result1;
            result[1] = result2;
        }

        if(result[0].length()==1){
            result[0] = "0" + result[0];
        }

        return result;
    }

    private ChromeDriver option() {
        System.setProperty("webdriver.chrome.driver", "/usr/bin/chromedriver");
        //System.setProperty("webdriver.chrome.whitelistedIps", "");
        //System.setProperty("webdriver.chrome.driver", "C:\\chromedriver_win32\\chromedriver.exe");

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless");
        options.addArguments("disable-gpu");
        options.addArguments("--disable-gpu");
        options.addArguments("--disable-popup-blocking");       //팝업안띄움
        options.addArguments("--blink-settings=imagesEnabled=false"); //이미지 다운 안받음
        options.addArguments("--remote-allow-origins=*");
        options.addArguments("window-size=1920x1080"); // 이거 안해주면 headless 때문에 안되고 useragent 넣어줘도 안됨
        options.addArguments("--no-sandbox");
        options.addArguments("disable-dev-shm-usage");
        options.addArguments("lang=ko");
        ChromeDriver driver = new ChromeDriver(options);

        return driver;
    }
}