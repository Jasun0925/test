package com.greeting.feature.greeting.repo;

import com.greeting.feature.greeting.entity.GreetingEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface GreetingRepository extends JpaRepository<GreetingEntity, UUID> {
}
