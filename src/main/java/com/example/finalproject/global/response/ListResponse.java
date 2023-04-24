package com.example.finalproject.global.response;


import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter

public class ListResponse<T> extends CommonResponse{


    private List<T> list;

}