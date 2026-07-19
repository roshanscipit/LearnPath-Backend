package com.doliuw.repository;

import com.doliuw.entity.MockCompany;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MockCompanyRepository extends JpaRepository<MockCompany, Long> {
}
