package com.oms.controller;

import com.oms.entity.Order;
import com.oms.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<Order> createOrder(@RequestBody Order order) {
        Order saved = orderService.createOrderAsync(order);
        return ResponseEntity.accepted().body(saved); // 202 Accepted — async processing
    }

    @PatchMapping("/{orderId}/status")
    public ResponseEntity<Void> updateStatus(@PathVariable String orderId, @RequestBody Map<String,String> body) {
        String status = body.get("status");
        String reason = body.get("reason");
        orderService.updateStatus(orderId, status, reason);
        return ResponseEntity.noContent().build();
    }

}


