package com.example.demo.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class TransactionHistoryService {

    private final RestTemplate restTemplate;

    @Value("${backend.base-url}")
    private String baseUrl;

    public TransactionHistoryService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public List<Map<String, Object>> getTransactionsByUserId(Integer userId) {
        Map response = restTemplate.getForObject(
                baseUrl + "/transaction_history/search/findByUserId?userId=" + userId,
                Map.class
        );

        return extractList(response, "transactionHistories");
    }

    public List<Map<String, Object>> getTransactionsByBranchId(Integer branchId) {
        Map response = restTemplate.getForObject(
                baseUrl + "/transaction_history/search/findByBranchId?branchId=" + branchId,
                Map.class
        );

        return extractList(response, "transactionHistories");
    }

    private List<Map<String, Object>> extractList(Map response, String key) {
        if (response == null || response.get("_embedded") == null) {
            return new ArrayList<>();
        }

        Map embedded = (Map) response.get("_embedded");

        List<Map<String, Object>> list =
                (List<Map<String, Object>>) embedded.getOrDefault(key, new ArrayList<>());

        for (Map<String, Object> item : list) {
            item.put("transactionId", extractIdFromSelfLink(item));
        }

        return list;
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