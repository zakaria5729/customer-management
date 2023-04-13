package com.zakaria.customer.model;

import com.zakaria.customer.entity.Customer;
import com.zakaria.customer.entity.InvalidCustomer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CustomerResult {
    private List<Customer> validCustomers;
    private List<InvalidCustomer> invalidCustomers;
    private Set<String> phoneSet;
    private Set<String> emailSet;

    public CustomerResult() {
        validCustomers = new ArrayList<>();
        invalidCustomers = new ArrayList<>();
        phoneSet = new HashSet<>();
        emailSet = new HashSet<>();
    }

    public List<Customer> getValidCustomers() {
        return validCustomers;
    }

    public List<InvalidCustomer> getInvalidCustomers() {
        return invalidCustomers;
    }

    public Set<String> getPhoneSet() {
        return phoneSet;
    }

    public Set<String> getEmailSet() {
        return emailSet;
    }
}