package com.zakaria.customer.controller;

import com.zakaria.customer.service.CustomerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequestMapping("/api/customer")
public class CustomerController {

    @Autowired
    private CustomerService customerService;

    @GetMapping("/process")
    public void processCustom() throws IOException {
        customerService.processCustomers("/home/zakaria/Downloads/customers.txt");
    }
}
