package com.nimbleways.springboilerplate.services.products;

import java.util.List;

import com.nimbleways.springboilerplate.services.implementations.NotificationService;
import com.nimbleways.springboilerplate.services.products.processors.ProductProcessor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.nimbleways.springboilerplate.entities.Product;
import com.nimbleways.springboilerplate.repositories.ProductRepository;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final List<ProductProcessor> productProcessors;
    private final ProductRepository productRepository;
    private final NotificationService notificationService;

    public void processProduct(Product product) {
        ProductProcessor processor = productProcessors.stream()
                .filter(p -> p.supports(product))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported product type: " + product.getType()));
        processor.process(product);
    }


    public void notifyDelay(int leadTime, Product product) {
        product.setLeadTime(leadTime);
        productRepository.save(product);
        notificationService.sendDelayNotification(leadTime, product.getName());
    }

}