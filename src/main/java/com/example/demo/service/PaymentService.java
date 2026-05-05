package com.example.demo.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class PaymentService {

    private final RestTemplate restTemplate;

    @Value("${backend.base-url}")
    private String baseUrl;

    public PaymentService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public List<Map<String, Object>> getPaymentsByUserId(Integer userId) {

        try {
            Map response = restTemplate.getForObject(
                    baseUrl + "/payments/search/findByUserid?userId=" + userId,
                    Map.class
            );

            if (response == null || response.get("_embedded") == null) {
                return new ArrayList<>();
            }

            Map embedded = (Map) response.get("_embedded");

            List<Map<String, Object>> payments =
                    (List<Map<String, Object>>) embedded.getOrDefault("paymentses", new ArrayList<>());

            for (Map<String, Object> payment : payments) {
                payment.put("paymentId", extractIdFromSelfLink(payment));
            }

            return payments;

        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private Integer extractIdFromSelfLink(Map<String, Object> entity) {
        try {
            Map<String, Object> links = (Map<String, Object>) entity.get("_links");
            Map<String, Object> self = (Map<String, Object>) links.get("self");
            String href = (String) self.get("href");

            return Integer.parseInt(href.substring(href.lastIndexOf("/") + 1));
        } catch (Exception e) {
            return null;
        }
    }
}