package com.pi.interview.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pi.interview.dto.InterviewResponse;
import com.pi.interview.dto.ScheduleInterviewRequest;
import com.pi.interview.dto.SlotResponse;
import com.pi.interview.enums.InterviewStatus;
import com.pi.interview.enums.InterviewType;
import com.pi.interview.enums.SlotStatus;
import com.pi.interview.service.InterviewService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InterviewController.class)
class InterviewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private InterviewService interviewService;

    @Test
    void getAvailableSlots_Success() throws Exception {
        UUID appId = UUID.randomUUID();
        SlotResponse slot = new SlotResponse(
                UUID.randomUUID(), UUID.randomUUID(),
                LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(1).plusMinutes(30),
                SlotStatus.AVAILABLE
        );

        when(interviewService.getAvailableSlotsForApplication(eq(appId), any()))
                .thenReturn(new PageImpl<>(List.of(slot)));

        mockMvc.perform(get("/api/v1/interviews/slots")
                        .param("applicationId", appId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].status").value("AVAILABLE"));
    }

    @Test
    void scheduleInterview_Success() throws Exception {
        UUID candidateId = UUID.randomUUID();
        UUID appId = UUID.randomUUID();
        UUID slotId = UUID.randomUUID();

        ScheduleInterviewRequest request = new ScheduleInterviewRequest(
                appId, slotId, 1, "Technical Round 1", InterviewType.VIDEO
        );

        InterviewResponse response = new InterviewResponse(
                UUID.randomUUID(), appId, candidateId, UUID.randomUUID(), UUID.randomUUID(), slotId,
                1, "Technical Round 1", InterviewType.VIDEO, InterviewStatus.SCHEDULED,
                "https://meet.366pi.com/int-1", LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(1).plusMinutes(30), null, LocalDateTime.now(), LocalDateTime.now()
        );

        when(interviewService.scheduleInterview(any(), eq(candidateId))).thenReturn(response);

        mockMvc.perform(post("/api/v1/interviews")
                        .header("X-Candidate-Id", candidateId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("SCHEDULED"))
                .andExpect(jsonPath("$.roundName").value("Technical Round 1"));
    }
}
