package com.nimbleways.springboilerplate.controllers;

import com.nimbleways.springboilerplate.entities.Order;
import com.nimbleways.springboilerplate.entities.Product;
import com.nimbleways.springboilerplate.repositories.OrderRepository;
import com.nimbleways.springboilerplate.repositories.ProductRepository;
import com.nimbleways.springboilerplate.services.implementations.NotificationService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;


import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// Specify the controller class you want to test
// This indicates to spring boot to only load UsersController into the context
// Which allows a better performance and needs to do less mocks
@SpringBootTest
@AutoConfigureMockMvc
public class OrderControllerIntegrationTests {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private NotificationService notificationService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductRepository productRepository;

    @Test
    public void processOrderShouldReturn() throws Exception {
            List<Product> allProducts = createProducts();
            Set<Product> orderItems = new HashSet<Product>(allProducts);
            Order order = createOrder(orderItems);
            productRepository.saveAll(allProducts);
            order = orderRepository.save(order);
            mockMvc.perform(post("/orders/{orderId}/processOrder", order.getId())
                            .contentType("application/json"))
                            .andExpect(status().isOk());
            Order resultOrder = orderRepository.findById(order.getId()).get();
            Assertions.assertEquals(resultOrder.getId(), order.getId());
    }

    private static Order createOrder(Set<Product> products) {
            Order order = new Order();
            order.setItems(products);
            return order;
    }

    private static List<Product> createProducts() {
            List<Product> products = new ArrayList<>();
            products.add(new Product(null, 15, 30, "NORMAL", "USB Cable", null, null, null));
            products.add(new Product(null, 10, 0, "NORMAL", "USB Dongle", null, null, null));
            products.add(new Product(null, 15, 30, "EXPIRABLE", "Butter", LocalDate.now().plusDays(26), null,
                            null));
            products.add(new Product(null, 90, 6, "EXPIRABLE", "Milk", LocalDate.now().minusDays(2), null, null));
            products.add(new Product(null, 15, 30, "SEASONAL", "Watermelon", null, LocalDate.now().minusDays(2),
                            LocalDate.now().plusDays(58)));
            products.add(new Product(null, 15, 30, "SEASONAL", "Grapes", null, LocalDate.now().plusDays(180),
                            LocalDate.now().plusDays(240)));
            return products;
    }

    @Test
    public void processOrder_withNormalProductInStock_shouldDecrementStock() throws Exception {
        Product product = new Product(null, 15, 10, "NORMAL", "USB Cable", null, null, null);
        Order order = createAndSaveOrder(product);

        processOrder(order.getId());

        Product updated = productRepository.findById(product.getId()).orElseThrow();
        Assertions.assertEquals(Integer.valueOf(9), updated.getAvailable());
        Mockito.verifyNoInteractions(notificationService);
    }

    @Test
    public void processOrder_withNormalProductOutOfStock_shouldNotifyDelay() throws Exception {
        Product product = new Product(null, 15, 0, "NORMAL", "USB Cable", null, null, null);

        Order order = createAndSaveOrder(product);
        processOrder(order.getId());

        Product updated = productRepository.findById(product.getId()).orElseThrow();
        Assertions.assertEquals(Integer.valueOf(0), updated.getAvailable());
        Mockito.verify(notificationService).sendDelayNotification(15, "USB Cable");
    }

    @Test
    public void processOrder_withSeasonalProductInSeasonAndInStock_shouldDecrementStock() throws Exception {
        Product product = new Product(null, 10, 5, "SEASONAL", "Watermelon", null,
                LocalDate.now().minusDays(10), LocalDate.now().plusDays(20));
        Order order = createAndSaveOrder(product);

        processOrder(order.getId());

        Product updated = productRepository.findById(product.getId()).orElseThrow();
        Assertions.assertEquals(Integer.valueOf(4), updated.getAvailable());
        Mockito.verifyNoInteractions(notificationService);
    }

    @Test
    public void processOrder_withSeasonalProductOutOfStockAndLeadTimeExceedsSeason_shouldNotifyOutOfStock() throws Exception {
        // Test for the case : leadTime = 30 jours, mais la saison finit dans 10 jours
        Product product = new Product(null, 30, 0, "SEASONAL", "Watermelon", null,
                LocalDate.now().minusDays(10), LocalDate.now().plusDays(10));
        Order order = createAndSaveOrder(product);

        processOrder(order.getId());

        Product updated = productRepository.findById(product.getId()).orElseThrow();
        Assertions.assertEquals(Integer.valueOf(0), updated.getAvailable());
        Mockito.verify(notificationService).sendOutOfStockNotification("Watermelon");
    }

    @Test
    public void processOrder_withSeasonalProductOutOfStockAndLeadTimeFitsInSeason_shouldNotifyDelay() throws Exception {
        // Test for the case : leadTime = 5 jours, et la saison finit dans 20 jours
        Product product = new Product(null, 5, 0, "SEASONAL", "Watermelon", null,
                LocalDate.now().minusDays(10), LocalDate.now().plusDays(20));
        Order order = createAndSaveOrder(product);

        processOrder(order.getId());

        Product updated = productRepository.findById(product.getId()).orElseThrow();
        Assertions.assertEquals(Integer.valueOf(0), updated.getAvailable());
        Mockito.verify(notificationService).sendDelayNotification(5, "Watermelon");
    }

    @Test
    public void processOrder_withSeasonalProductBeforeSeason_shouldNotifyOutOfStock() throws Exception {
        // Test for the case : la saison commence dans 5 jours
        Product product = new Product(null, 5, 10, "SEASONAL", "Watermelon", null,
                LocalDate.now().plusDays(5), LocalDate.now().plusDays(30));
        Order order = createAndSaveOrder(product);

        processOrder(order.getId());

        Mockito.verify(notificationService).sendOutOfStockNotification("Watermelon");
    }

    @Test
    public void processOrder_withExpirableProductValid_shouldDecrementStock() throws Exception {
        Product product = new Product(null, 10, 5, "EXPIRABLE", "Butter", LocalDate.now().plusDays(10), null, null);
        Order order = createAndSaveOrder(product);
        processOrder(order.getId());
        Product updated = productRepository.findById(product.getId()).orElseThrow();
        Assertions.assertEquals(Integer.valueOf(4), updated.getAvailable());
        Mockito.verifyNoInteractions(notificationService);
    }
    @Test
    public void processOrder_withExpirableProductExpired_shouldSetStockToZeroAndNotify() throws Exception {
        LocalDate expiryDate = LocalDate.now().minusDays(1);
        Product product = new Product(null, 10, 5, "EXPIRABLE", "Milk", expiryDate, null, null);
        Order order = createAndSaveOrder(product);
        processOrder(order.getId());
        Product updated = productRepository.findById(product.getId()).orElseThrow();
        Assertions.assertEquals(Integer.valueOf(0), updated.getAvailable());
        Mockito.verify(notificationService).sendExpirationNotification("Milk", expiryDate);
    }

    @Test
    public void processOrder_whenOrderNotFound_shouldReturn404() throws Exception {
        mockMvc.perform(post("/orders/{orderId}/processOrder", 999999L)
                        .contentType("application/json"))
                .andExpect(status().isNotFound());
    }




    /***
     * Helper method to create and process orders (for tests only)
     * @param product
     * @return Order
     */
    private Order createAndSaveOrder(Product product) {
            Product savedProduct = productRepository.save(product);
            Order order = new Order();
            order.setItems(Set.of(savedProduct));
            return orderRepository.save(order);
    }

    private void processOrder(Long orderId) throws Exception {
        mockMvc.perform(post("/orders/{orderId}/processOrder", orderId)
                        .contentType("application/json"))
                .andExpect(status().isOk());
    }

}
