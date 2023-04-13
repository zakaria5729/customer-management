package com.zakaria.customer.service;

import com.zakaria.customer.entity.Customer;
import com.zakaria.customer.entity.InvalidCustomer;
import com.zakaria.customer.model.CustomerResult;
import com.zakaria.customer.repository.CustomerRepository;
import com.zakaria.customer.repository.InvalidCustomerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;

@Service
public class CustomerService {

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private InvalidCustomerRepository invalidCustomerRepository;

    private final int CHUNK_SIZE = 100000; // number of lines to process in each chunk
    private final ExecutorService executorService = Executors.newFixedThreadPool(8);

    public void processCustomers(String filePath) {
        long startTime = System.currentTimeMillis();

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line = null;
            List<String> chunks = new ArrayList<>();

            int chunkCount = 0;
            while ((line = reader.readLine()) != null) {
                chunks.add(line);
                if (chunks.size() == CHUNK_SIZE) {
                    processChunk(chunks, ++chunkCount);
                    chunks = Collections.synchronizedList(new ArrayList<>());
                }
            }

            // process the remaining data
            if (!chunks.isEmpty()) {
                processChunk(chunks, ++chunkCount);
            }

            executorService.shutdown();
            executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS); // wait for all tasks to finish

            // combine the results from all threads
            CustomerResult combinedResult = new CustomerResult();
            for (Future<CustomerResult> future : results) {
                CustomerResult result = future.get();
                combinedResult.getValidCustomers().addAll(result.getValidCustomers());
                combinedResult.getInvalidCustomers().addAll(result.getInvalidCustomers());
                combinedResult.getPhoneSet().addAll(result.getPhoneSet());
                combinedResult.getEmailSet().addAll(result.getEmailSet());
            }

            List<Customer> validCustomers = combinedResult.getValidCustomers();
            List<InvalidCustomer> invalidCustomers = combinedResult.getInvalidCustomers();

            customerRepository.saveAllAndFlush(validCustomers);
            invalidCustomerRepository.saveAllAndFlush(invalidCustomers);

            String outputDir = "/home/zakaria/Downloads/custom_directory";
            exportValidCustomers(outputDir, validCustomers);
            exportInvalidCustomers(outputDir, invalidCustomers);

            long endTime = System.currentTimeMillis();
            System.out.println("Total execution time: " + (endTime - startTime) + " milliseconds");

        } catch (IOException | InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }

    private final List<Future<CustomerResult>> results = new ArrayList<>();

    private void processChunk(List<String> data, int chunkCount) {
        results.add(executorService.submit(() -> {
            CustomerResult result = new CustomerResult();

            for (String line : data) {
                String[] records = line.split(",");
                String firstName = records[0];
                String lastName = records[1];
                String city = records[2];
                String state = records[3];
                String zipCode = records[4];
                String phone = records[5];
                String email = records[6];
                String ipAddress = null;

                if(records.length == 8) {
                    ipAddress = records[7];
                }

                if (isValidCustomer(email, phone)) {
                    Customer customer = new Customer();
                    customer.setFirstName(firstName);
                    customer.setLastName(lastName);
                    customer.setCity(city);
                    customer.setState(state);
                    customer.setZipCode(zipCode);
                    customer.setPhoneNumber(phone);
                    customer.setEmail(email);
                    customer.setIpAddress(ipAddress);

                    result.getValidCustomers().add(customer);
                    result.getPhoneSet().add(customer.getPhoneNumber());
                    result.getEmailSet().add(customer.getEmail());
                } else {
                    InvalidCustomer invalidCustomer = new InvalidCustomer();
                    invalidCustomer.setFirstName(firstName);
                    invalidCustomer.setLastName(lastName);
                    invalidCustomer.setCity(city);
                    invalidCustomer.setState(state);
                    invalidCustomer.setZipCode(zipCode);
                    invalidCustomer.setPhoneNumber(phone);
                    invalidCustomer.setEmail(email);
                    invalidCustomer.setIpAddress(ipAddress);

                    result.getInvalidCustomers().add(invalidCustomer);
                }

            }

            return result;
        }));
    }

    private boolean isValidCustomer(String email, String phoneNumber) {
        return isValidPhoneNumber(phoneNumber) && isValidEmail(email);
    }

    private boolean isValidPhoneNumber(String phone) {
        String regex = "(\\d{3})?[- ]?(\\d{3})[- ]?(\\d{4})";
        return phone.matches(regex);
    }

    private boolean isValidEmail(String email) {
        String regex = "[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}";
        return email.matches(regex);
    }

    public void exportValidCustomers(String outputDir, List<Customer> validCustomers) {
        long startTime = System.currentTimeMillis();

        int batchSize = 100000;
        int batchCount = validCustomers.size() / batchSize;
        if (validCustomers.size() % batchSize != 0) {
            batchCount++;
        }

        for (int i = 0; i < batchCount; i++) {
            int startIndex = i * batchSize;
            int endIndex = Math.min((i + 1) * batchSize, validCustomers.size());
            List<Customer> batch = validCustomers.subList(startIndex, endIndex);
            String fileName = String.format("%s/valid_customers_%d.txt", outputDir, i + 1);
            exportCustomers(batch, null, fileName);
        }

        long endTime = System.currentTimeMillis();
        System.out.println("Total valid customers exported time: " + (endTime - startTime) + " milliseconds");
    }

    public void exportInvalidCustomers(String outputDir, List<InvalidCustomer> invalidCustomers) {
        long startTime = System.currentTimeMillis();

        String fileName = String.format("%s/invalid_customers.txt", outputDir);
        exportCustomers(null, invalidCustomers, fileName);

        long endTime = System.currentTimeMillis();
        System.out.println("Total invalid customers exported time: " + (endTime - startTime) + " milliseconds");
    }

    private void exportCustomers(List<Customer> customers, List<InvalidCustomer> invalidCustomers, String fileName) {
        if (customers != null && !customers.isEmpty()) {
            try (PrintWriter writer = new PrintWriter(new FileWriter(fileName))) {
                for (Customer customer : customers) {
                    writer.println(
                            customer.getFirstName() + "," +
                            customer.getLastName() + "," +
                            customer.getCity() + "," +
                            customer.getState() + "," +
                            customer.getZipCode() + "," +
                            customer.getPhoneNumber() + "," +
                            customer.getEmail() + "," +
                            customer.getIpAddress()
                    );
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (invalidCustomers != null && !invalidCustomers.isEmpty()) {
            try (PrintWriter writer = new PrintWriter(new FileWriter(fileName))) {
                for (InvalidCustomer invalidCustomer : invalidCustomers) {
                    writer.println(
                            invalidCustomer.getFirstName() + "," +
                            invalidCustomer.getLastName() + "," +
                            invalidCustomer.getCity() + "," +
                            invalidCustomer.getState() + "," +
                            invalidCustomer.getZipCode() + "," +
                            invalidCustomer.getPhoneNumber() + "," +
                            invalidCustomer.getEmail() + "," +
                            invalidCustomer.getIpAddress()
                    );
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}

