package com.example.finalproject.presubscribe.repository;

import com.example.finalproject.presubscribe.entity.Presubscribe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PresubscribeRepository extends JpaRepository<Presubscribe, Long> {
    Optional<Presubscribe> findByEmail(String email);

}
