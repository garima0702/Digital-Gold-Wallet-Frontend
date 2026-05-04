package com.example.demo.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class UserService {

    private final RestTemplate restTemplate;

    @Value("${backend.base-url}")
    private String baseUrl;

    public UserService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public Map<String, Object> getAllUsersPaginated(int page, int size) {
        Map response = restTemplate.getForObject(
                baseUrl + "/users?page=" + page + "&size=" + size,
                Map.class
        );

        return buildPagedResponse(response, "userses");
    }

    public Map<String, Object> findUsersByName(String name, int page, int size) {
        Map response = restTemplate.getForObject(
                baseUrl + "/users/search/findByName?name=" + name + "&page=" + page + "&size=" + size,
                Map.class
        );

        return buildPagedResponse(response, "userses");
    }

    public Map<String, Object> findUsersByCity(String city, int page, int size) {
        Map response = restTemplate.getForObject(
                baseUrl + "/users/search/findByAddress_City?city=" + city + "&page=" + page + "&size=" + size,
                Map.class
        );

        return buildPagedResponse(response, "userses");
    }

    public Map<String, Object> findUsersByState(String state, int page, int size) {
        Map response = restTemplate.getForObject(
                baseUrl + "/users/search/findByAddress_State?state=" + state + "&page=" + page + "&size=" + size,
                Map.class
        );

        return buildPagedResponse(response, "userses");
    }

    public Map<String, Object> getUserById(Integer userId) {
        Map<String, Object> user =
                restTemplate.getForObject(baseUrl + "/users/" + userId, Map.class);

        if (user != null) {
            user.put("userId", userId);
        }

        return user;
    }

    public void createUser(String name, String email, String balance) {
        Map<String, Object> user = new HashMap<>();
        user.put("name", name);
        user.put("email", email);
        user.put("balance", Double.parseDouble(balance));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(user, headers);

        restTemplate.exchange(
                baseUrl + "/users",
                HttpMethod.POST,
                request,
                Map.class
        );
    }

    public void updateUser(Integer userId, String name, String email, String balance) {
        Map<String, Object> user = new HashMap<>();
        user.put("name", name);
        user.put("email", email);
        user.put("balance", Double.parseDouble(balance));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(user, headers);

        restTemplate.exchange(
                baseUrl + "/users/" + userId,
                HttpMethod.PATCH,
                request,
                Map.class
        );
    }

    private Map<String, Object> buildPagedResponse(Map response, String embeddedKey) {
        Map<String, Object> result = new HashMap<>();

        List<Map<String, Object>> data = new ArrayList<>();

        if (response != null && response.get("_embedded") != null) {
            Map embedded = (Map) response.get("_embedded");

            data = (List<Map<String, Object>>) embedded.getOrDefault(
                    embeddedKey,
                    new ArrayList<>()
            );

            for (Map<String, Object> item : data) {
                item.put("userId", extractIdFromSelfLink(item));
            }
        }

        Map pageInfo = response != null && response.get("page") != null
                ? (Map) response.get("page")
                : new HashMap();

        result.put("data", data);
        result.put("size", pageInfo.getOrDefault("size", 0));
        result.put("totalElements", pageInfo.getOrDefault("totalElements", 0));
        result.put("totalPages", pageInfo.getOrDefault("totalPages", 1));
        result.put("number", pageInfo.getOrDefault("number", 0));

        return result;
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