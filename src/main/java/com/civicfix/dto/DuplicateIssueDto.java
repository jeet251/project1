package com.civicfix.dto;

import com.civicfix.entity.Complaint;
import com.civicfix.entity.ComplaintStatus;

public class DuplicateIssueDto {

    private Long id;
    private String complaintId;
    private String title;
    private String category;
    private String address;
    private ComplaintStatus status;
    private double distanceMeters;
    private int upvoteCount;

    public DuplicateIssueDto() {
    }

    public static DuplicateIssueDto fromEntity(Complaint complaint, double distanceMeters) {
        DuplicateIssueDto dto = new DuplicateIssueDto();
        dto.setId(complaint.getId());
        dto.setComplaintId(complaint.getComplaintId());
        dto.setTitle(complaint.getTitle());
        dto.setCategory(complaint.getCategory());
        dto.setAddress(complaint.getAddress());
        dto.setStatus(complaint.getStatus());
        dto.setDistanceMeters(Math.round(distanceMeters * 10.0) / 10.0);
        dto.setUpvoteCount(complaint.getUpvoteCount());
        return dto;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getComplaintId() {
        return complaintId;
    }

    public void setComplaintId(String complaintId) {
        this.complaintId = complaintId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public ComplaintStatus getStatus() {
        return status;
    }

    public void setStatus(ComplaintStatus status) {
        this.status = status;
    }

    public double getDistanceMeters() {
        return distanceMeters;
    }

    public void setDistanceMeters(double distanceMeters) {
        this.distanceMeters = distanceMeters;
    }

    public int getUpvoteCount() {
        return upvoteCount;
    }

    public void setUpvoteCount(int upvoteCount) {
        this.upvoteCount = upvoteCount;
    }
}
