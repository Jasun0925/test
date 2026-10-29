package com.greeting.feature.greeting.service;

import com.greeting.exception.GreetingNotFoundException;
import com.greeting.feature.greeting.dto.CreateGreetingRequest;
import com.greeting.feature.greeting.dto.GreetingResponse;
import com.greeting.feature.greeting.entity.GreetingEntity;
import com.greeting.feature.greeting.repo.GreetingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GreetingService {

    private final GreetingRepository greetingRepository;

    /**
     * Create and persist a new greeting.
     *
     * @param request the validated creation request
     * @return the UUID of the newly created greeting
     */
    @Transactional
    public UUID createGreeting(CreateGreetingRequest request) {
        UUID id = UUID.randomUUID();
        GreetingEntity entity = GreetingEntity.builder()
                .text(request.getText())
                .build();
        entity.setId(id);
        greetingRepository.save(entity);
        return id;
    }

    /**
     * Retrieve a greeting by its identifier.
     *
     * @param id the greeting UUID
     * @return the greeting response DTO
     * @throws GreetingNotFoundException if no greeting exists with the given id
     */
    @Transactional(readOnly = true)
    public GreetingResponse getGreeting(UUID id) {
        GreetingEntity entity = greetingRepository.findById(id)
                .orElseThrow(() -> new GreetingNotFoundException("Greeting not found"));
        return GreetingResponse.builder()
                .id(entity.getId())
                .text(entity.getText())
                .build();
    }
}
