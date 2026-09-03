package com.nimbleways.springboilerplate.services.orders;

import com.nimbleways.springboilerplate.dto.product.ProcessOrderResponse;

public interface IOrderService {

    ProcessOrderResponse processOrder(Long orderId);
}
