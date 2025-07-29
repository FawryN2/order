package com.fawry_fridges.order.clients;

import com.fawry_fridges.order.dto.ProductDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.RestTemplate;

public class ProductClient {

    private final RestTemplate restTemplate;

    @Value("${product.service.base-url}")
    private String productServiceBaseUrl;

    public ProductClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public ProductDto getProductById(String productId) {
        String url = productServiceBaseUrl + "api/products/" + productId;
        return restTemplate.getForObject(url, ProductDto.class);
    }
}
