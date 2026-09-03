package com.nimbleways.springboilerplate.services.products.processors;

import com.nimbleways.springboilerplate.entities.Product;
import com.nimbleways.springboilerplate.repositories.ProductRepository;
import com.nimbleways.springboilerplate.services.implementations.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class ExpirableProductProcessor implements ProductProcessor{

    private final ProductRepository productRepository;
    private final NotificationService notificationService;

    @Override
    public boolean supports(Product product) {
        return "EXPIRABLE".equals(product.getType());
    }

    @Override
    public void process(Product product) {
        LocalDate now = LocalDate.now();
        if (product.getAvailable() > 0 && product.getExpiryDate().isAfter(now)) {
            product.setAvailable(product.getAvailable() - 1);
            productRepository.save(product);
        } else {
            notificationService.sendExpirationNotification(product.getName(), product.getExpiryDate());
            product.setAvailable(0);
            productRepository.save(product);
        }
    }
}
