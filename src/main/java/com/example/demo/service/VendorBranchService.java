package com.example.demo.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class VendorBranchService {

    private final RestTemplate restTemplate;

    @Value("${backend.base-url}")
    private String baseUrl;

    public VendorBranchService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public Map<String, Object> getAllVendorBranchesPaginated(int page, int size) {
        Map response = restTemplate.getForObject(
                baseUrl + "/vendor_branches?page=" + page + "&size=" + size,
                Map.class
        );

        return buildPagedBranchResponse(response);
    }

    public Map<String, Object> findByVendorIdPaginated(Integer vendorId, int page, int size) {
        Map response = restTemplate.getForObject(
                baseUrl + "/vendor_branches/search/by_vendor?vendorId=" + vendorId + "&page=" + page + "&size=" + size,
                Map.class
        );

        return buildPagedBranchResponse(response);
    }

    public Map<String, Object> findByCityPaginated(String city, int page, int size) {
        Map response = restTemplate.getForObject(
                baseUrl + "/vendor_branches/search/by_city?city=" + city + "&page=" + page + "&size=" + size,
                Map.class
        );

        return buildPagedBranchResponse(response);
    }

    public Map<String, Object> findByStatePaginated(String state, int page, int size) {
        Map response = restTemplate.getForObject(
                baseUrl + "/vendor_branches/search/by_state?state=" + state + "&page=" + page + "&size=" + size,
                Map.class
        );

        return buildPagedBranchResponse(response);
    }

    public Map<String, Object> getVendorBranchById(Integer branchId) {
        Map<String, Object> branch =
                restTemplate.getForObject(baseUrl + "/vendor_branches/" + branchId, Map.class);

        if (branch != null) {
            branch.put("branchId", branchId);
            enrichBranch(branch);
        }

        return branch;
    }

    public Map<String, Object> getLinkedVendor(Map<String, Object> branch) {
        String vendorUrl = getLink(branch, "vendor");

        if (vendorUrl == null) {
            return new HashMap<>();
        }

        return restTemplate.getForObject(vendorUrl, Map.class);
    }

    public List<Map<String, Object>> getAllVendors() {
        Map response = restTemplate.getForObject(baseUrl + "/vendors?size=100", Map.class);
        List<Map<String, Object>> vendors = extractFirstEmbeddedList(response);

        for (Map<String, Object> vendor : vendors) {
            vendor.put("vendorId", extractIdFromSelfLink(vendor));
        }

        return vendors;
    }

    public List<Map<String, Object>> getAllAddresses() {
        Map response = restTemplate.getForObject(baseUrl + "/address?size=100", Map.class);
        List<Map<String, Object>> addresses = extractFirstEmbeddedList(response);

        for (Map<String, Object> address : addresses) {
            address.put("addressId", extractIdFromSelfLink(address));

            String addressText =
                    address.getOrDefault("street", "") + ", " +
                    address.getOrDefault("city", "") + ", " +
                    address.getOrDefault("state", "") + ", " +
                    address.getOrDefault("country", "");

            address.put("addressText", addressText);
        }

        return addresses;
    }

    public void createBranch(Integer vendorId, Integer addressId, String quantity) {
        Map<String, Object> branch = new HashMap<>();

        branch.put("quantity", Double.parseDouble(quantity));
        branch.put("vendor", baseUrl + "/vendors/" + vendorId);
        branch.put("address", baseUrl + "/address/" + addressId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(branch, headers);

        restTemplate.exchange(
                baseUrl + "/vendor_branches",
                HttpMethod.POST,
                request,
                Map.class
        );
    }

    public void updateBranch(Integer branchId, String quantity) {
        Map<String, Object> branch = new HashMap<>();
        branch.put("quantity", Double.parseDouble(quantity));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(branch, headers);

        restTemplate.exchange(
                baseUrl + "/vendor_branches/" + branchId,
                HttpMethod.PATCH,
                request,
                Map.class
        );
    }

    private Map<String, Object> buildPagedBranchResponse(Map response) {
        Map<String, Object> result = new HashMap<>();

        List<Map<String, Object>> branches = new ArrayList<>();

        if (response != null && response.get("_embedded") != null) {
            Map embedded = (Map) response.get("_embedded");

            branches = (List<Map<String, Object>>) embedded.getOrDefault(
                    "vendorBrancheses",
                    new ArrayList<>()
            );

            for (Map<String, Object> branch : branches) {
                branch.put("branchId", extractIdFromSelfLink(branch));
                enrichBranch(branch);
            }
        }

        Map pageInfo = response != null && response.get("page") != null
                ? (Map) response.get("page")
                : new HashMap();

        result.put("data", branches);
        result.put("size", pageInfo.getOrDefault("size", 0));
        result.put("totalElements", pageInfo.getOrDefault("totalElements", 0));
        result.put("totalPages", pageInfo.getOrDefault("totalPages", 1));
        result.put("number", pageInfo.getOrDefault("number", 0));

        return result;
    }

    private List<Map<String, Object>> extractFirstEmbeddedList(Map response) {
        if (response == null || response.get("_embedded") == null) {
            return new ArrayList<>();
        }

        Map embedded = (Map) response.get("_embedded");

        if (embedded.isEmpty()) {
            return new ArrayList<>();
        }

        Object firstValue = embedded.values().iterator().next();

        if (firstValue instanceof List) {
            return (List<Map<String, Object>>) firstValue;
        }

        return new ArrayList<>();
    }

    private void enrichBranch(Map<String, Object> branch) {
        enrichVendor(branch);
        enrichAddress(branch);
    }

    private void enrichVendor(Map<String, Object> branch) {
        try {
            String vendorUrl = getLink(branch, "vendor");

            if (vendorUrl == null) {
                branch.put("vendorName", "Not available");
                return;
            }

            Map<String, Object> vendor = restTemplate.getForObject(vendorUrl, Map.class);

            if (vendor == null) {
                branch.put("vendorName", "Not available");
                return;
            }

            branch.put("vendorName", vendor.getOrDefault("vendorName", "Not available"));
        } catch (Exception e) {
            branch.put("vendorName", "Not available");
        }
    }

    private void enrichAddress(Map<String, Object> branch) {
        try {
            String addressUrl = getLink(branch, "address");

            if (addressUrl == null) {
                branch.put("addressText", "Not available");
                return;
            }

            Map<String, Object> address = restTemplate.getForObject(addressUrl, Map.class);

            if (address == null) {
                branch.put("addressText", "Not available");
                return;
            }

            String addressText =
                    address.getOrDefault("street", "") + ", " +
                    address.getOrDefault("city", "") + ", " +
                    address.getOrDefault("state", "") + ", " +
                    address.getOrDefault("country", "");

            branch.put("addressText", addressText);
        } catch (Exception e) {
            branch.put("addressText", "Not available");
        }
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