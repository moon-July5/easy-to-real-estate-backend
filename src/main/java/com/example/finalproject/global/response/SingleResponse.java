package com.example.finalproject.global.response;



import lombok.Getter;
import lombok.Setter;

@Setter
@Getter

public class SingleResponse<T> extends CommonResponse{


    private T data;

}
