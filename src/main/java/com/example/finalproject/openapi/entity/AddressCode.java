package com.example.finalproject.openapi.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;


@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class AddressCode {
    @Id
    @Column(name = "legdong_code")
    private String legdongCode; // 법정동 코드

    @Column(name = "legdong_name")
    private String legdongName; // 지역주소
}
