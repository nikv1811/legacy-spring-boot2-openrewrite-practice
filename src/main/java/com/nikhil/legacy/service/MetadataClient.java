package com.nikhil.legacy.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class MetadataClient {

    private final RestTemplate restTemplate;

    public MetadataClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public String fetchPostTitle(long id) {
        return restTemplate.getForObject(
            "https://jsonplaceholder.typicode.com/posts/{id}",
            String.class,
            id
        );
    }
}
