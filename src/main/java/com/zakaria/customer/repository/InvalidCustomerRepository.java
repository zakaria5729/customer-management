package com.zakaria.customer.repository;

import com.zakaria.customer.entity.InvalidCustomer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InvalidCustomerRepository extends JpaRepository<InvalidCustomer, Long> {

}