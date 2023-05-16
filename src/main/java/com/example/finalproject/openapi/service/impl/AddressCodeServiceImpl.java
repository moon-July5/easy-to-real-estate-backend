package com.example.finalproject.openapi.service.impl;

import com.example.finalproject.global.response.CommonResponse;
import com.example.finalproject.global.response.ResponseService;
import com.example.finalproject.openapi.entity.AddressCode;
import com.example.finalproject.openapi.repository.AddressCodeRepository;
import com.example.finalproject.openapi.service.AddressCodeService;
import lombok.RequiredArgsConstructor;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

@RequiredArgsConstructor
@Service
public class AddressCodeServiceImpl implements AddressCodeService {
    /*
     * 서울, 부산, 대구, 인천, 광주, 대전, 울산, 경기도,
     * 강원도, 충북, 충청, 전북, 전남, 경북, 경남, 제주도
     * */
    private String[] code = {"11","26", "27", "28", "29", "30", "31", "41",
            "42", "43","44", "45", "46", "47", "48", "50"};

    @Value("${api.url}")
    private String regCodeApiUrl;

    private final AddressCodeRepository addressCodeRepository;
    private final ResponseService responseService;

    // 주소에서 아파트 이름 조회
    @Override
    public String findAptName(String address) {
        String[] addressList = address.split(" ");

        int count = 0;
        if (addressList[2].endsWith("동")) {
            count = 3;
        } else {
            count = 4;
        }

        for(int i=count; i<addressList.length; i++){
            if(addressList[i].contains("필지")){
                count = i;
                break;
            }
        }

        return addressList[count + 1].trim();
    }

    @Override
    public String findJibun(String address) {
        String[] addressList = address.split(" ");

        int count = 0;

        if (addressList[2].endsWith("동")) {
            count = 3;
        } else {
            count = 4;
        }

        String result = null;

        if(count==3)
            result = addressList[0]+" "+addressList[1]+" "+addressList[2]+" "+addressList[3];
        else if(count==4)
            result = addressList[0]+" "+addressList[1]+" "+addressList[2]+" "+addressList[3]+" "+addressList[4];

        return result;
    }

    // 법정동 코드 조회
    @Override
    public String findAddressCode(String address) {

        String[] addressList = address.split(" ");
        StringBuilder sb = new StringBuilder();
        int count = 0;
        if (addressList[2].endsWith("동")) {
            count = 3;
        } else {
            count = 4;
        }

        if(addressList[count].endsWith("리"))
            count = 3;

        for (int i = 0; i < count; i++) {
            sb.append(addressList[i]);
            sb.append(" ");
        }
        address = sb.toString().trim();

        return addressCodeRepository.findByLegdongName(address)
                .orElseThrow(IllegalArgumentException::new).getLegdongCode();
    }

    // 전 지역 법정동 코드 db에 저장
    @Override
    public CommonResponse regCodeApi() throws Exception {
        for(int i=0; i<code.length; i++){
            String result = "";

            try {
                URL url = new URL(regCodeApiUrl + code[i] + "*");
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.setRequestProperty("Content-type", "application/json");

                BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream(), "UTF-8"));

                result = br.readLine();

                JSONParser parser = new JSONParser();
                JSONObject jsonObject = (JSONObject) parser.parse(result);
                JSONArray array = (JSONArray) jsonObject.get("regcodes");

                for (int j = 0; j < array.size(); j++) {
                    JSONObject object = (JSONObject) array.get(j);
                    String legdongName = (String) object.get("name");
                    String legdongCode = (String) object.get("code");

                    addressCodeRepository.save(AddressCode.builder()
                            .legdongName(legdongName)
                            .legdongCode(legdongCode)
                            .build());
                }
            }catch (Exception e){
                return responseService.getFailResponse(404, "페이지를 찾을 수 없습니다");
            }
        }
        return responseService.getSuccessResponse();
    }
}
