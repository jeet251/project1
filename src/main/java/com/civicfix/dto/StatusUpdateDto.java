package com.civicfix.dto;

import com.civicfix.entity.ComplaintStatus;
import jakarta.validation.constraints.NotNull;

public class StatusUpdateDto {

    @NotNull(message = "Status is required")
    private ComplaintStatus status;

    private String note;

    private String rejectionReason;

    public StatusUpdateDto() {
    }

    public StatusUpdateDto(ComplaintStatus status, String note) {
        this.status = status;
        this.note = note;
    }

    public ComplaintStatus getStatus() {
        return status;
    }

    public void setStatus(ComplaintStatus status) {
        this.status = status;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }
}
