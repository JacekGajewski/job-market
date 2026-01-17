package com.jobmarket.service;

import com.jobmarket.dto.CityDto;
import com.jobmarket.dto.CreateCityRequest;
import com.jobmarket.entity.TrackedCity;
import com.jobmarket.exception.CityNotFoundException;
import com.jobmarket.exception.DuplicateCityException;
import com.jobmarket.mapper.CityMapper;
import com.jobmarket.repository.TrackedCityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CityService {

    private final TrackedCityRepository cityRepository;
    private final CityMapper cityMapper;

    public List<CityDto> findAll() {
        return cityRepository.findAll().stream()
                .map(cityMapper::toDto)
                .toList();
    }

    public List<CityDto> findAllActive() {
        return cityRepository.findByActiveTrue().stream()
                .map(cityMapper::toDto)
                .toList();
    }

    @Transactional
    public CityDto create(CreateCityRequest request) {
        if (cityRepository.existsBySlug(request.slug())) {
            throw new DuplicateCityException(request.slug());
        }

        TrackedCity city = TrackedCity.builder()
                .name(request.name())
                .slug(request.slug())
                .active(false)
                .build();

        TrackedCity saved = cityRepository.save(city);
        log.info("Created new city: {}", saved.getSlug());
        return cityMapper.toDto(saved);
    }

    @Transactional
    public void delete(Long id) {
        if (!cityRepository.existsById(id)) {
            throw new CityNotFoundException(id);
        }
        cityRepository.deleteById(id);
        log.info("Deleted city with id: {}", id);
    }

    @Transactional
    public CityDto setActive(Long id, boolean active) {
        TrackedCity city = cityRepository.findById(id)
                .orElseThrow(() -> new CityNotFoundException(id));
        city.setActive(active);
        TrackedCity saved = cityRepository.save(city);
        log.info("Set city {} active={}", saved.getSlug(), active);
        return cityMapper.toDto(saved);
    }
}
