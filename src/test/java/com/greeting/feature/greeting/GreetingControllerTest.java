package com.greeting.feature.greeting;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.greeting.exception.GreetingNotFoundException;
import com.greeting.feature.greeting.dto.CreateGreetingRequest;
import com.greeting.feature.greeting.dto.GreetingResponse;
import com.greeting.feature.greeting.service.GreetingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(GreetingController.class)
class GreetingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GreetingService greetingService;

    @Autowired
    private ObjectMapper objectMapper;

    // ---------------------------------------------------------------
    // AC-01: POST /greetings with {"text": "Hello"} returns 201 with id
    // ---------------------------------------------------------------

    @Test
    void createGreeting_validText_returns201WithId() throws Exception {
        UUID id = UUID.randomUUID();
        when(greetingService.createGreeting(any(CreateGreetingRequest.class))).thenReturn(id);

        mockMvc.perform(post("/greetings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\": \"Hello\"}"))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(id.toString()));
    }

    // ---------------------------------------------------------------
    // AC-02: GET /greetings/{id} with valid id returns 200 with id and text
    // ---------------------------------------------------------------

    @Test
    void getGreeting_existingId_returns200WithIdAndText() throws Exception {
        UUID id = UUID.randomUUID();
        GreetingResponse response = GreetingResponse.builder()
                .id(id)
                .text("Hello")
                .build();
        when(greetingService.getGreeting(id)).thenReturn(response);

        mockMvc.perform(get("/greetings/{id}", id))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.text").value("Hello"));
    }

    // ---------------------------------------------------------------
    // AC-03: GET /greetings/{nonexistent} returns 404
    // ---------------------------------------------------------------

    @Test
    void getGreeting_nonExistingId_returns404WithErrorMessage() throws Exception {
        UUID id = UUID.randomUUID();
        when(greetingService.getGreeting(id))
                .thenThrow(new GreetingNotFoundException("Greeting not found"));

        mockMvc.perform(get("/greetings/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("Greeting not found"));
    }

    // ---------------------------------------------------------------
    // AC-04: POST with {"text": ""} returns 400
    // ---------------------------------------------------------------

    @Test
    void createGreeting_emptyText_returns400() throws Exception {
        mockMvc.perform(post("/greetings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\": \"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").exists());

        verify(greetingService, never()).createGreeting(any());
    }

    // ---------------------------------------------------------------
    // AC-05: POST with no text field returns 400
    // ---------------------------------------------------------------

    @Test
    void createGreeting_missingTextField_returns400() throws Exception {
        mockMvc.perform(post("/greetings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").exists());

        verify(greetingService, never()).createGreeting(any());
    }

    // ---------------------------------------------------------------
    // AC-06: POST with 41-char text returns 400
    // ---------------------------------------------------------------

    @Test
    void createGreeting_textTooLong41Chars_returns400() throws Exception {
        String longText = "a".repeat(41);
        mockMvc.perform(post("/greetings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\": \"" + longText + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").exists());

        verify(greetingService, never()).createGreeting(any());
    }

    // ---------------------------------------------------------------
    // AC-07: POST with 1-char text returns 201
    // ---------------------------------------------------------------

    @Test
    void createGreeting_singleCharText_returns201() throws Exception {
        UUID id = UUID.randomUUID();
        when(greetingService.createGreeting(any(CreateGreetingRequest.class))).thenReturn(id);

        mockMvc.perform(post("/greetings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\": \"A\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(id.toString()));
    }

    // ---------------------------------------------------------------
    // AC-08: POST with 40-char text returns 201
    // ---------------------------------------------------------------

    @Test
    void createGreeting_exactly40CharsText_returns201() throws Exception {
        UUID id = UUID.randomUUID();
        when(greetingService.createGreeting(any(CreateGreetingRequest.class))).thenReturn(id);

        String text40 = "a".repeat(40);
        mockMvc.perform(post("/greetings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\": \"" + text40 + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(id.toString()));
    }

    // ---------------------------------------------------------------
    // Invalid UUID format in GET path returns 400
    // ---------------------------------------------------------------

    @Test
    void getGreeting_invalidUuidFormat_returns400() throws Exception {
        mockMvc.perform(get("/greetings/not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void getGreeting_plainTextId_returns400() throws Exception {
        mockMvc.perform(get("/greetings/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    // ---------------------------------------------------------------
    // Malformed JSON body returns 400
    // ---------------------------------------------------------------

    @Test
    void createGreeting_malformedJson_returns400() throws Exception {
        mockMvc.perform(post("/greetings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{invalid json"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("Malformed request body"));
    }

    // ---------------------------------------------------------------
    // Additional validation edge cases
    // ---------------------------------------------------------------

    @Test
    void createGreeting_whitespaceOnlyText_returns400() throws Exception {
        mockMvc.perform(post("/greetings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\": \"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());

        verify(greetingService, never()).createGreeting(any());
    }

    @Test
    void createGreeting_nullTextField_returns400() throws Exception {
        mockMvc.perform(post("/greetings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\": null}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());

        verify(greetingService, never()).createGreeting(any());
    }

    // ---------------------------------------------------------------
    // Error response format: only "error" field, no stack traces
    // ---------------------------------------------------------------

    @Test
    void getGreeting_notFound_errorResponseContainsOnlyErrorField() throws Exception {
        UUID id = UUID.randomUUID();
        when(greetingService.getGreeting(id))
                .thenThrow(new GreetingNotFoundException("Greeting not found"));

        mockMvc.perform(get("/greetings/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Greeting not found"))
                .andExpect(jsonPath("$.trace").doesNotExist())
                .andExpect(jsonPath("$.status").doesNotExist())
                .andExpect(jsonPath("$.path").doesNotExist());
    }

    @Test
    void createGreeting_validationError_errorResponseContainsOnlyErrorField() throws Exception {
        mockMvc.perform(post("/greetings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\": \"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.trace").doesNotExist())
                .andExpect(jsonPath("$.status").doesNotExist())
                .andExpect(jsonPath("$.path").doesNotExist());
    }

    // ---------------------------------------------------------------
    // Controller delegates to service correctly
    // ---------------------------------------------------------------

    @Test
    void createGreeting_delegatesToService() throws Exception {
        UUID id = UUID.randomUUID();
        when(greetingService.createGreeting(any(CreateGreetingRequest.class))).thenReturn(id);

        mockMvc.perform(post("/greetings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\": \"Hello\"}"))
                .andExpect(status().isCreated());

        verify(greetingService).createGreeting(any(CreateGreetingRequest.class));
    }

    @Test
    void getGreeting_delegatesToService() throws Exception {
        UUID id = UUID.randomUUID();
        GreetingResponse response = GreetingResponse.builder()
                .id(id)
                .text("Hello")
                .build();
        when(greetingService.getGreeting(id)).thenReturn(response);

        mockMvc.perform(get("/greetings/{id}", id))
                .andExpect(status().isOk());

        verify(greetingService).getGreeting(id);
    }

    // ---------------------------------------------------------------
    // POST with text exceeding 40 chars by a large margin
    // ---------------------------------------------------------------

    @Test
    void createGreeting_veryLongText_returns400() throws Exception {
        String veryLongText = "a".repeat(200);
        mockMvc.perform(post("/greetings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\": \"" + veryLongText + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }
}
