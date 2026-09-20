package com.civicfix.dto;

import com.civicfix.entity.ComplaintStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class ComplaintUpdateDto {

    @NotNull(message = "Status is required")
    private ComplaintStatus status;

    @NotBlank(message = "Progress message/notes cannot be blank")
    private String message;

    private String proofImagePath;

    public ComplaintUpdateDto() {
    }

    public ComplaintUpdateDto(ComplaintStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    public ComplaintStatus getStatus() {
        return status;
    }

    public void setStatus(ComplaintStatus status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getProofImagePath() {
        return proofImagePath;
    }

    public void setProofImagePath(String proofImagePath) {
        this.proofImagePath = proofImagePath;
    }
}
