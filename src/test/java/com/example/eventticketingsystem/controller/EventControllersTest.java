package com.example.eventticketingsystem.controller;

import com.example.eventticketingsystem.dto.common.PagedResponse;
import com.example.eventticketingsystem.dto.event.response.EventResponse;
import com.example.eventticketingsystem.exception.GlobalExceptionHandler;
import com.example.eventticketingsystem.exception.ResourceNotFoundException;
import com.example.eventticketingsystem.service.EventService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class EventControllersTest {

    @Mock
    private EventService eventService;

    @Test
    void listEvents_usesDefaultPaginationAndSort() throws Exception {
        EventController controller = new EventController(eventService);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        when(eventService.listActiveEvents(eq(25), eq(0), eq("eventDateTime,asc")))
                .thenReturn(new PagedResponse<>(List.of(), 25, 0, 0L));

        mockMvc.perform(get("/api/v1/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.limit").value(25))
                .andExpect(jsonPath("$.offset").value(0));

        verify(eventService).listActiveEvents(eq(25), eq(0), eq("eventDateTime,asc"));
    }

    @Test
    void getEvent_returns404WhenServiceThrowsNotFound() throws Exception {
        EventController controller = new EventController(eventService);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        when(eventService.getEventById(eq(999L)))
                .thenThrow(new ResourceNotFoundException("The event with id '999' was not found."));

        mockMvc.perform(get("/api/v1/events/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("ResourceNotFound"));
    }

    @Test
    void createEvent_returns400WhenBodyInvalid() throws Exception {
        AdminEventController controller = new AdminEventController(eventService);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        mockMvc.perform(post("/api/v1/admin/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("ValidationFailed"));
    }

    @Test
    void createEvent_returns400ForInvalidEnumOrDateFormat() throws Exception {
        AdminEventController controller = new AdminEventController(eventService);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        String invalidJson = """
                {
                  \"name\": \"Spring Fest\",
                  \"venue\": \"Central Park\",
                  \"eventDateTime\": \"invalid-date\"
                }
                """;

        mockMvc.perform(post("/api/v1/admin/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("ValidationFailed"));
    }
}
