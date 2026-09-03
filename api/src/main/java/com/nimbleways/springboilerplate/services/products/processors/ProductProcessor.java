package com.nimbleways.springboilerplate.services.products.processors;

import com.nimbleways.springboilerplate.entities.Product;

public interface ProductProcessor {
    boolean supports(Product product);
    void process(Product product);
}
