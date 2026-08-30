package com.restaurant.order_service.controller;

import com.restaurant.order_service.dto.KOTResponse;
import com.restaurant.order_service.entity.KOTStatus;
import com.restaurant.order_service.service.KOTService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/kots")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class KOTController {

    private final KOTService kotService;


    // =====================================================
    // GENERATE KOT
    // =====================================================

    @PostMapping("/order/{orderId}")
    public ResponseEntity<KOTResponse> generateKOT(
            @PathVariable Long orderId,
            @RequestParam(required = false) String generatedBy
    ) {

        KOTResponse response =
                kotService.generateKOT(
                        orderId,
                        generatedBy
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }


    // =====================================================
    // START PREPARING
    // =====================================================

    @PutMapping("/{kotId}/preparing")
    public ResponseEntity<KOTResponse> startPreparing(
            @PathVariable Long kotId
    ) {

        return ResponseEntity.ok(
                kotService.startPreparing(kotId)
        );
    }


    // =====================================================
    // MARK READY
    // =====================================================

    @PutMapping("/{kotId}/ready")
    public ResponseEntity<KOTResponse> markReady(
            @PathVariable Long kotId
    ) {

        return ResponseEntity.ok(
                kotService.markReady(kotId)
        );
    }


    // =====================================================
    // GET KOT
    // =====================================================

    @GetMapping("/{kotId}")
    public ResponseEntity<KOTResponse> getKOT(
            @PathVariable Long kotId
    ) {

        return ResponseEntity.ok(
                kotService.getKOTById(kotId)
        );
    }


    // =====================================================
    // GET KOT BY ORDER
    // =====================================================

    @GetMapping("/order/{orderId}")
    public ResponseEntity<KOTResponse> getKOTByOrder(
            @PathVariable Long orderId
    ) {

        return ResponseEntity.ok(
                kotService.getKOTByOrderId(orderId)
        );
    }


    // =====================================================
    // GET ALL KOTS
    // =====================================================

    @GetMapping
    public ResponseEntity<List<KOTResponse>> getAllKOTs() {

        return ResponseEntity.ok(
                kotService.getAllKOTs()
        );
    }


    // =====================================================
    // GET KOTS BY STATUS
    // =====================================================

    @GetMapping("/status/{status}")
    public ResponseEntity<List<KOTResponse>> getKOTsByStatus(
            @PathVariable KOTStatus status
    ) {

        return ResponseEntity.ok(
                kotService.getKOTsByStatus(status)
        );
    }
}