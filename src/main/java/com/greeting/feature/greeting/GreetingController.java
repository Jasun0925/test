package com.greeting.feature.greeting;

import com.greeting.feature.greeting.dto.CreateGreetingRequest;
import com.greeting.feature.greeting.dto.CreateGreetingResponse;
import com.greeting.feature.greeting.dto.GreetingResponse;
import com.greeting.feature.greeting.service.GreetingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/greetings")
@RequiredArgsConstructor
public class GreetingController {

    private final GreetingService greetingService;

    /**
     * Create a new greeting.
     *
     * @param request the greeting text payload
     * @return the created greeting's id
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CreateGreetingResponse createGreeting(
            @Valid @RequestBody CreateGreetingRequest request) {
        UUID id = greetingService.createGreeting(request);
        return CreateGreetingResponse.builder()
                .id(id)
                .build();
    }

    /**
     * Retrieve a greeting by its identifier.
     *
     * @param id the greeting UUID
     * @return the greeting id and text
     */
    @GetMapping("/{id}")
    public GreetingResponse getGreeting(@PathVariable UUID id) {
        return greetingService.getGreeting(id);
    }
}
