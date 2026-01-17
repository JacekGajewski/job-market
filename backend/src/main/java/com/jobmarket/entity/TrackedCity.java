package com.jobmarket.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tracked_city", indexes = {
    @Index(name = "idx_tracked_city_slug", columnList = "slug", unique = true),
    @Index(name = "idx_tracked_city_active", columnList = "active")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrackedCity extends BaseEntity {

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "slug", nullable = false, unique = true, length = 100)
    private String slug;

    @Column(name = "active", nullable = false)
    @Builder.Default
    private Boolean active = false;
}
