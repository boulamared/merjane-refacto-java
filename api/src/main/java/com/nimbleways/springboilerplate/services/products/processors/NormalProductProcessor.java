package com.nimbleways.springboilerplate.services.products.processors;

import com.nimbleways.springboilerplate.entities.Product;
import com.nimbleways.springboilerplate.repositories.ProductRepository;
import com.nimbleways.springboilerplate.services.implementations.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NormalProductProcessor implements ProductProcessor{

    private final ProductRepository productRepository;
    private final NotificationService notificationService;

    @Override
    public boolean supports(Product product) {
        return "NORMAL".equals(product.getType());
    }

    @Override
    public void process(Product product) {
        if (product.getAvailable() > 0) {
            product.setAvailable(product.getAvailable() - 1);
            productRepository.save(product);
        } else {
            int leadTime = product.getLeadTime();
            if (leadTime > 0) {
                product.setLeadTime(leadTime);
                productRepository.save(product);
                notificationService.sendDelayNotification(leadTime, product.getName());
            }
        }
    }
}
