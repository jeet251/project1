package com.civicfix.dto;

import jakarta.validation.constraints.NotNull;

public class AssignComplaintDto {

    @NotNull(message = "Department ID is required")
    private Long departmentId;

    private Long officerId; // Optional specific officer

    private String note;

    public AssignComplaintDto() {
    }

    public AssignComplaintDto(Long departmentId, Long officerId, String note) {
        this.departmentId = departmentId;
        this.officerId = officerId;
        this.note = note;
    }

    public Long getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(Long departmentId) {
        this.departmentId = departmentId;
    }

    public Long getOfficerId() {
        return officerId;
    }

    public void setOfficerId(Long officerId) {
        this.officerId = officerId;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
