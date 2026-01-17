package com.jobmarket.controller;

import com.jobmarket.dto.CityDto;
import com.jobmarket.dto.CreateCityRequest;
import com.jobmarket.service.CityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cities")
@RequiredArgsConstructor
@Tag(name = "Cities", description = "City management endpoints")
public class CityController {

    private final CityService cityService;

    @GetMapping
    @Operation(summary = "List all tracked cities")
    public ResponseEntity<List<CityDto>> getAllCities() {
        return ResponseEntity.ok(cityService.findAll());
    }

    @PostMapping
    @Operation(summary = "Add a new city to track")
    public ResponseEntity<CityDto> createCity(
            @Valid @RequestBody CreateCityRequest request) {
        CityDto created = cityService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove a tracked city")
    public ResponseEntity<Void> deleteCity(@PathVariable Long id) {
        cityService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/active")
    @Operation(summary = "Set city active status")
    public ResponseEntity<CityDto> setActive(
            @PathVariable Long id,
            @RequestParam boolean active) {
        return ResponseEntity.ok(cityService.setActive(id, active));
    }
}
