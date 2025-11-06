package com.oms.inventory.controller;

import com.oms.inventory.entity.Inventory;
import com.oms.inventory.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @GetMapping("/{productId}")
    public ResponseEntity<Optional<Inventory>> getInventory(@PathVariable Long productId) {
        return ResponseEntity.ok(inventoryService.getByProductId(productId));
    }

    @PutMapping("/{productId}")
    public ResponseEntity<Inventory> updateStock(@PathVariable Long productId,
                                                 @RequestParam Integer quantity) {
        return ResponseEntity.ok(inventoryService.updateStock(productId, quantity));
    }

    @GetMapping("/check/{productId}")
    public ResponseEntity<Boolean> checkStock(@PathVariable Long productId,
                                              @RequestParam Integer qty) {
        return ResponseEntity.ok(inventoryService.isInStock(productId, qty));
    }
}

