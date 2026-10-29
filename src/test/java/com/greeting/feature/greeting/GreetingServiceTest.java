package com.greeting.feature.greeting;

import com.greeting.exception.GreetingNotFoundException;
import com.greeting.feature.greeting.dto.CreateGreetingRequest;
import com.greeting.feature.greeting.dto.GreetingResponse;
import com.greeting.feature.greeting.entity.GreetingEntity;
import com.greeting.feature.greeting.repo.GreetingRepository;
import com.greeting.feature.greeting.service.GreetingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GreetingServiceTest {

    @Mock
    private GreetingRepository greetingRepository;

    @InjectMocks
    private GreetingService greetingService;

    // ---------------------------------------------------------------
    // createGreeting tests
    // ---------------------------------------------------------------

    @Test
    void createGreeting_savesEntityAndReturnsUuid() {
        CreateGreetingRequest request = CreateGreetingRequest.builder()
                .text("Hello")
                .build();

        when(greetingRepository.save(any(GreetingEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UUID result = greetingService.createGreeting(request);

        assertThat(result).isNotNull();

        ArgumentCaptor<GreetingEntity> captor = ArgumentCaptor.forClass(GreetingEntity.class);
        verify(greetingRepository).save(captor.capture());

        GreetingEntity saved = captor.getValue();
        assertThat(saved.getId()).isEqualTo(result);
        assertThat(saved.getText()).isEqualTo("Hello");
    }

    @Test
    void createGreeting_generatesUniqueUuid() {
        CreateGreetingRequest request = CreateGreetingRequest.builder()
                .text("Hello")
                .build();

        when(greetingRepository.save(any(GreetingEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UUID result1 = greetingService.createGreeting(request);
        UUID result2 = greetingService.createGreeting(request);

        assertThat(result1).isNotNull();
        assertThat(result2).isNotNull();
        assertThat(result1).isNotEqualTo(result2);
    }

    @Test
    void createGreeting_callsRepositorySaveExactlyOnce() {
        CreateGreetingRequest request = CreateGreetingRequest.builder()
                .text("Hello")
                .build();

        when(greetingRepository.save(any(GreetingEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        greetingService.createGreeting(request);

        verify(greetingRepository, times(1)).save(any(GreetingEntity.class));
    }

    @Test
    void createGreeting_setsTextFromRequest() {
        String expectedText = "Greetings from the test";
        CreateGreetingRequest request = CreateGreetingRequest.builder()
                .text(expectedText)
                .build();

        when(greetingRepository.save(any(GreetingEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        greetingService.createGreeting(request);

        ArgumentCaptor<GreetingEntity> captor = ArgumentCaptor.forClass(GreetingEntity.class);
        verify(greetingRepository).save(captor.capture());

        assertThat(captor.getValue().getText()).isEqualTo(expectedText);
    }

    @Test
    void createGreeting_entityIdMatchesReturnedUuid() {
        CreateGreetingRequest request = CreateGreetingRequest.builder()
                .text("Hello")
                .build();

        when(greetingRepository.save(any(GreetingEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UUID returnedId = greetingService.createGreeting(request);

        ArgumentCaptor<GreetingEntity> captor = ArgumentCaptor.forClass(GreetingEntity.class);
        verify(greetingRepository).save(captor.capture());

        assertThat(captor.getValue().getId()).isEqualTo(returnedId);
    }

    // ---------------------------------------------------------------
    // getGreeting tests
    // ---------------------------------------------------------------

    @Test
    void getGreeting_existingId_returnsResponseWithCorrectIdAndText() {
        UUID id = UUID.randomUUID();
        GreetingEntity entity = GreetingEntity.builder()
                .text("Hello")
                .build();
        entity.setId(id);

        when(greetingRepository.findById(id)).thenReturn(Optional.of(entity));

        GreetingResponse response = greetingService.getGreeting(id);

        assertThat(response.getId()).isEqualTo(id);
        assertThat(response.getText()).isEqualTo("Hello");
    }

    @Test
    void getGreeting_existingId_callsRepositoryFindByIdWithCorrectId() {
        UUID id = UUID.randomUUID();
        GreetingEntity entity = GreetingEntity.builder()
                .text("Hello")
                .build();
        entity.setId(id);

        when(greetingRepository.findById(id)).thenReturn(Optional.of(entity));

        greetingService.getGreeting(id);

        verify(greetingRepository, times(1)).findById(id);
    }

    @Test
    void getGreeting_nonExistingId_throwsGreetingNotFoundException() {
        UUID id = UUID.randomUUID();
        when(greetingRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> greetingService.getGreeting(id))
                .isInstanceOf(GreetingNotFoundException.class)
                .hasMessage("Greeting not found");
    }

    @Test
    void getGreeting_nonExistingId_exceptionHasNotFoundStatus() {
        UUID id = UUID.randomUUID();
        when(greetingRepository.findById(id)).thenReturn(Optional.empty());

        try {
            greetingService.getGreeting(id);
        } catch (GreetingNotFoundException ex) {
            assertThat(ex.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
            return;
        }
        throw new AssertionError("Expected GreetingNotFoundException to be thrown");
    }

    @Test
    void getGreeting_returnsTextMatchingEntity() {
        UUID id = UUID.randomUUID();
        String expectedText = "A specific greeting text";
        GreetingEntity entity = GreetingEntity.builder()
                .text(expectedText)
                .build();
        entity.setId(id);

        when(greetingRepository.findById(id)).thenReturn(Optional.of(entity));

        GreetingResponse response = greetingService.getGreeting(id);

        assertThat(response.getText()).isEqualTo(expectedText);
    }
}
