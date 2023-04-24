package com.example.finalproject.global.response;



import lombok.Getter;
import lombok.Setter;

@Getter
@Setter

public class CommonResponse {


    private boolean isSuccess;


    private int code;


    private String message;

    public CommonResponse(){}
    public CommonResponse(int code,String msg){
        this.code = code;
        this.message = msg;
    };


}
