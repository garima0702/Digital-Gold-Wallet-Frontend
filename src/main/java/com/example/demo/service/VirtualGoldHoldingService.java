package com.example.demo.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class VirtualGoldHoldingService {

    private final RestTemplate restTemplate;

    @Value("${backend.base-url}")
    private String baseUrl;

    public VirtualGoldHoldingService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public List<Map<String, Object>> getHoldingsByBranchId(Integer branchId) {
        Map response = restTemplate.getForObject(
                baseUrl + "/virtual-gold-holdings/search/by-branch?branchId=" + branchId,
                Map.class
        );

        return extractHoldings(response);
    }

    public List<Map<String, Object>> getHoldingsByUserId(Integer userId) {
        Map response = restTemplate.getForObject(
                baseUrl + "/virtual-gold-holdings/search/by-user?userId=" + userId,
                Map.class
        );

        return extractHoldings(response);
    }

    private List<Map<String, Object>> extractHoldings(Map response) {
        if (response == null || response.get("_embedded") == null) {
            return new ArrayList<>();
        }

        Map embedded = (Map) response.get("_embedded");

        List<Map<String, Object>> holdings =
                (List<Map<String, Object>>) embedded.getOrDefault("virtualGoldHoldings", new ArrayList<>());

        for (Map<String, Object> holding : holdings) {
            holding.put("holdingId", extractIdFromSelfLink(holding));
            enrichHolding(holding);
        }

        return holdings;
    }

    private void enrichHolding(Map<String, Object> holding) {
        holding.put("userName", getUserName(holding));
        holding.put("vendorName", getVendorNameFromBranch(holding));
        holding.put("branchAddressText", getBranchAddressText(holding));
    }

    private String getUserName(Map<String, Object> holding) {
        try {
            String userUrl = getLink(holding, "user");
            Map<String, Object> user = restTemplate.getForObject(userUrl, Map.class);
            return String.valueOf(user.getOrDefault("name", "Not available"));
        } catch (Exception e) {
            return "Not available";
        }
    }

    private String getVendorNameFromBranch(Map<String, Object> holding) {
        try {
            String branchUrl = getLink(holding, "branch");
            Map<String, Object> branch = restTemplate.getForObject(branchUrl, Map.class);

            String vendorUrl = getLink(branch, "vendor");
            Map<String, Object> vendor = restTemplate.getForObject(vendorUrl, Map.class);

            return String.valueOf(vendor.getOrDefault("vendorName", "Not available"));
        } catch (Exception e) {
            return "Not available";
        }
    }

    private String getBranchAddressText(Map<String, Object> holding) {
        try {
            String branchUrl = getLink(holding, "branch");
            Map<String, Object> branch = restTemplate.getForObject(branchUrl, Map.class);

            String addressUrl = getLink(branch, "address");
            Map<String, Object> address = restTemplate.getForObject(addressUrl, Map.class);

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