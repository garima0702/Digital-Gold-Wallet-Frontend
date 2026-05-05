package com.example.demo.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class AddressService {

    private final RestTemplate restTemplate;

    public AddressService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public Map<String, Object> getAddressByUrl(String addressUrl) {
        try {
            if (addressUrl == null || addressUrl.isBlank()) {
                return null;
            }

            return restTemplate.getForObject(addressUrl, Map.class);

        } catch (Exception e) {
            return null;
        }
    }
}