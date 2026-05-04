package com.example.demo.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class PhysicalGoldTransactionService {

    private final RestTemplate restTemplate;

    @Value("${backend.base-url}")
    private String baseUrl;

    public PhysicalGoldTransactionService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public List<Map<String, Object>> getTransactionsByUserId(Integer userId) {
        Map response = restTemplate.getForObject(
                baseUrl + "/physical_gold_transactions/search/findByUserId?userId=" + userId,
                Map.class
        );

        return extractPhysicalTransactions(response);
    }

    public List<Map<String, Object>> getTransactionsByBranchId(Integer branchId) {
        Map response = restTemplate.getForObject(
                baseUrl + "/physical_gold_transactions/search/findByBranchId?branchId=" + branchId,
                Map.class
        );

        return extractPhysicalTransactions(response);
    }

    private List<Map<String, Object>> extractPhysicalTransactions(Map response) {
        if (response == null || response.get("_embedded") == null) {
            return new ArrayList<>();
        }

        Map embedded = (Map) response.get("_embedded");

        List<Map<String, Object>> transactions =
                (List<Map<String, Object>>) embedded.getOrDefault(
                        "physicalGoldTransactionses",
                        new ArrayList<>()
                );

        for (Map<String, Object> tx : transactions) {
            tx.put("transactionId", extractIdFromSelfLink(tx));
            enrichTransaction(tx);
        }

        return transactions;
    }

    private void enrichTransaction(Map<String, Object> tx) {
        tx.put("userName", getUserName(tx));
        tx.put("deliveryAddressText", getAddressTextFromLink(tx, "deliveryAddress"));
        tx.put("branchAddressText", getBranchAddressText(tx));
    }

    private String getUserName(Map<String, Object> tx) {
        try {
            String userUrl = getLink(tx, "user");

            if (userUrl == null) {
                return "Not available";
            }

            Map<String, Object> user = restTemplate.getForObject(userUrl, Map.class);

            if (user == null) {
                return "Not available";
            }

            return String.valueOf(user.getOrDefault("name", "Not available"));

        } catch (Exception e) {
            return "Not available";
        }
    }

    private String getAddressTextFromLink(Map<String, Object> entity, String linkName) {
        try {
            String addressUrl = getLink(entity, linkName);

            if (addressUrl == null) {
                return "Not available";
            }

            Map<String, Object> address = restTemplate.getForObject(addressUrl, Map.class);

            return formatAddress(address);

        } catch (Exception e) {
            return "Not available";
        }
    }

    private String getBranchAddressText(Map<String, Object> tx) {
        try {
            String branchUrl = getLink(tx, "branch");

            if (branchUrl == null) {
                return "Not available";
            }

            Map<String, Object> branch = restTemplate.getForObject(branchUrl, Map.class);

            if (branch == null) {
                return "Not available";
            }

            String branchAddressUrl = getLink(branch, "address");

            if (branchAddressUrl == null) {
                return "Not available";
            }

            Map<String, Object> address = restTemplate.getForObject(branchAddressUrl, Map.class);

            return formatAddress(address);

        } catch (Exception e) {
            return "Not available";
        }
    }

    private String formatAddress(Map<String, Object> address) {
        if (address == null) {
            return "Not available";
        }

        return address.getOrDefault("street", "") + ", " +
                address.getOrDefault("city", "") + ", " +
                address.getOrDefault("state", "") + ", " +
                address.getOrDefault("country", "");
    }

    private String getLink(Map<String, Object> entity, String linkName) {
        try {
            Map<String, Object> links = (Map<String, Object>) entity.get("_links");
            Map<String, Object> link = (Map<String, Object>) links.get(linkName);
            return (String) link.get("href");
        } catch (Exception e) {
            return null;
        }
    }

    private Integer extractIdFromSelfLink(Map<String, Object> entity) {
        try {
            String href = getLink(entity, "self");
            return Integer.parseInt(href.substring(href.lastIndexOf("/") + 1));
        } catch (Exception e) {
            return null;
        }
    }
}