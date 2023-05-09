package com.example.finalproject.openapi.repository;


import com.example.finalproject.openapi.entity.AddressCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AddressCodeRepository extends JpaRepository<AddressCode, String> {

    Optional<AddressCode> findByLegdongName(String legdongName); // 주소로 법정동 코드 조회
}
