package com.example.demo.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class AddressService {

    private final RestTemplate restTemplate;

    @Value("${backend.base-url}")
    private String baseUrl;

    public AddressService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public Map<String, Object> getAddressByUrl(String addressUrl) {
        return restTemplate.getForObject(addressUrl, Map.class);
    }

    public Map<String, Object> getAddressById(Integer addressId) {
        return restTemplate.getForObject(baseUrl + "/address/" + addressId, Map.class);
    }
}