package com.pi.interview.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pi.interview.dto.CreateSlotRequest;
import com.pi.interview.dto.InterviewResponse;
import com.pi.interview.dto.SlotResponse;
import com.pi.interview.dto.UpdateInterviewStatusRequest;
import com.pi.interview.enums.InterviewStatus;
import com.pi.interview.enums.InterviewType;
import com.pi.interview.enums.SlotStatus;
import com.pi.interview.service.InterviewService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminInterviewController.class)
class AdminInterviewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private InterviewService interviewService;

    @Test
    void createSlot_Success() throws Exception {
        UUID interviewerId = UUID.randomUUID();
        LocalDateTime start = LocalDateTime.now().plusDays(1);
        LocalDateTime end = start.plusMinutes(45);

        CreateSlotRequest request = new CreateSlotRequest(interviewerId, start, end);
        SlotResponse response = new SlotResponse(
                UUID.randomUUID(), interviewerId, start, end, SlotStatus.AVAILABLE
        );

        when(interviewService.createSlot(any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/admin/interviews/slots")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("AVAILABLE"))
                .andExpect(jsonPath("$.interviewerId").value(interviewerId.toString()));
    }

    @Test
    void updateStatus_Success() throws Exception {
        UUID interviewId = UUID.randomUUID();
        UpdateInterviewStatusRequest request = new UpdateInterviewStatusRequest(
                InterviewStatus.CONFIRMED, "Candidate confirmed attendance"
        );

        InterviewResponse response = new InterviewResponse(
                interviewId, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), 1, "Technical Round 1", InterviewType.VIDEO, InterviewStatus.CONFIRMED,
                "https://meet.366pi.com/int-1", LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(1).plusMinutes(30), "Candidate confirmed attendance",
                LocalDateTime.now(), LocalDateTime.now()
        );

        when(interviewService.updateStatus(eq(interviewId), eq(InterviewStatus.CONFIRMED), any(), any()))
                .thenReturn(response);

        mockMvc.perform(patch("/api/v1/admin/interviews/{interviewId}/status", interviewId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }
}
