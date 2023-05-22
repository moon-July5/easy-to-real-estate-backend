package com.example.finalproject.presubscribe.service.impl;

import com.example.finalproject.global.response.CommonResponse;
import com.example.finalproject.global.response.ResponseService;
import com.example.finalproject.presubscribe.dto.PreSubscribeReqDTO;
import com.example.finalproject.presubscribe.entity.Presubscribe;
import com.example.finalproject.presubscribe.repository.PresubscribeRepository;
import com.example.finalproject.presubscribe.service.PresubscribeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RequiredArgsConstructor
@Service
public class PresubscribeImpl implements PresubscribeService {
    private final PresubscribeRepository repository;
    private final ResponseService responseService;

    // 사전 등록 기능
    @Override
    public CommonResponse register(PreSubscribeReqDTO reqDTO){
        if(!isValidEmail(reqDTO.getEmail()))
            return responseService.getFailResponse(400, "이메일 형식이 올바르지 않습니다");

        Optional<Presubscribe> customer = repository.findByEmail(reqDTO.getEmail());

        if(!customer.isPresent()){ // 이메일이 존재하지 않으면 이메일 저장
            Presubscribe newCustomer = Presubscribe.builder().email(reqDTO.getEmail()).count(1).build();
            repository.save(newCustomer);
        } else { // 이메일이 존재하면 count+1
            int count = customer.get().getCount() + 1;
            customer.get().setCount(count);
            repository.save(customer.get());
        }

        return responseService.getSuccessResponse();
    }

    // 이메일 형식 체크
    public boolean isValidEmail(String email){
        String regex = "^[_a-z0-9-]+(.[_a-z0-9-]+)*@(?:\\w+\\.)+\\w+$";
        Pattern p = Pattern.compile(regex);
        Matcher m = p.matcher(email);

        if(!m.matches())
            return false;

        return true;
    }
}
