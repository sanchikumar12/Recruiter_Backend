package com.pi.interview.mapper;

import com.pi.interview.dto.InterviewHistoryResponse;
import com.pi.interview.dto.InterviewResponse;
import com.pi.interview.dto.InterviewerResponse;
import com.pi.interview.dto.SlotResponse;
import com.pi.interview.entity.Interview;
import com.pi.interview.entity.InterviewHistory;
import com.pi.interview.entity.InterviewSlot;
import com.pi.interview.entity.Interviewer;
import org.springframework.stereotype.Component;

@Component
public class InterviewMapper {

    public InterviewResponse toResponse(Interview interview) {
        if (interview == null) {
            return null;
        }
        return new InterviewResponse(
                interview.getId(),
                interview.getApplicationId(),
                interview.getCandidateId(),
                interview.getJobId(),
                interview.getInterviewerId(),
                interview.getSlotId(),
                interview.getRoundNumber(),
                interview.getRoundName(),
                interview.getInterviewType(),
                interview.getStatus(),
                interview.getMeetingLink(),
                interview.getScheduledStartTime(),
                interview.getScheduledEndTime(),
                interview.getNotes(),
                interview.getCreatedAt(),
                interview.getUpdatedAt()
        );
    }

    public SlotResponse toSlotResponse(InterviewSlot slot) {
        if (slot == null) {
            return null;
        }
        return new SlotResponse(
                slot.getId(),
                slot.getInterviewerId(),
                slot.getStartTime(),
                slot.getEndTime(),
                slot.getStatus()
        );
    }

    public InterviewerResponse toInterviewerResponse(Interviewer interviewer) {
        if (interviewer == null) {
            return null;
        }
        return new InterviewerResponse(
                interviewer.getId(),
                interviewer.getUserId(),
                interviewer.getName(),
                interviewer.getEmail(),
                interviewer.isActive(),
                interviewer.getCreatedAt(),
                interviewer.getUpdatedAt()
        );
    }

    public InterviewHistoryResponse toHistoryResponse(InterviewHistory history) {
        if (history == null) {
            return null;
        }
        return new InterviewHistoryResponse(
                history.getId(),
                history.getInterviewId(),
                history.getAction(),
                history.getOldStartTime(),
                history.getOldEndTime(),
                history.getNewStartTime(),
                history.getNewEndTime(),
                history.getOldStatus(),
                history.getNewStatus(),
                history.getPerformedBy(),
                history.getRemarks(),
                history.getCreatedAt()
        );
    }
}
