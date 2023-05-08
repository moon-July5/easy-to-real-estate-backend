package com.example.finalproject.openapi.repository;


import com.example.finalproject.openapi.entity.AddressCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AddressCodeRepository extends JpaRepository<AddressCode, String> {
}
