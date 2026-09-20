package com.civicfix.dto;

import com.civicfix.entity.Complaint;
import com.civicfix.entity.ComplaintPriority;
import com.civicfix.entity.ComplaintStatus;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ComplaintResponseDto {

    private Long id;
    private String complaintId;
    private String title;
    private String description;
    private String category;
    private ComplaintPriority priority;
    private ComplaintStatus status;
    private String rejectionReason;
    private Double latitude;
    private Double longitude;
    private String address;
    private String citizenName;
    private String citizenEmail;
    private String citizenPhone;
    private Long departmentId;
    private String departmentName;
    private Long assignedOfficerId;
    private String assignedOfficerName;
    private int upvoteCount;
    private boolean hasUpvoted = false;
    private List<ImageDto> images = new ArrayList<>();
    private List<UpdateDto> updates = new ArrayList<>();
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static class ImageDto {
        private Long id;
        private String filePath;
        private String fileName;

        public ImageDto(Long id, String filePath, String fileName) {
            this.id = id;
            this.filePath = filePath;
            this.fileName = fileName;
        }

        public Long getId() { return id; }
        public String getFilePath() { return filePath; }
        public String getFileName() { return fileName; }
    }

    public static class UpdateDto {
        private Long id;
        private ComplaintStatus status;
        private String message;
        private String updatedByName;
        private String proofImagePath;
        private LocalDateTime createdAt;

        public UpdateDto(Long id, ComplaintStatus status, String message, String updatedByName, String proofImagePath, LocalDateTime createdAt) {
            this.id = id;
            this.status = status;
            this.message = message;
            this.updatedByName = updatedByName;
            this.proofImagePath = proofImagePath;
            this.createdAt = createdAt;
        }

        public Long getId() { return id; }
        public ComplaintStatus getStatus() { return status; }
        public String getMessage() { return message; }
        public String getUpdatedByName() { return updatedByName; }
        public String getProofImagePath() { return proofImagePath; }
        public LocalDateTime getCreatedAt() { return createdAt; }
    }

    public static ComplaintResponseDto fromEntity(Complaint complaint, boolean maskCitizenInfo, boolean hasUpvoted) {
        ComplaintResponseDto dto = new ComplaintResponseDto();
        dto.setId(complaint.getId());
        dto.setComplaintId(complaint.getComplaintId());
        dto.setTitle(complaint.getTitle());
        dto.setDescription(complaint.getDescription());
        dto.setCategory(complaint.getCategory());
        dto.setPriority(complaint.getPriority());
        dto.setStatus(complaint.getStatus());
        dto.setRejectionReason(complaint.getRejectionReason());
        dto.setLatitude(complaint.getLatitude());
        dto.setLongitude(complaint.getLongitude());
        dto.setAddress(complaint.getAddress());
        dto.setUpvoteCount(complaint.getUpvoteCount());
        dto.setHasUpvoted(hasUpvoted);
        dto.setCreatedAt(complaint.getCreatedAt());
        dto.setUpdatedAt(complaint.getUpdatedAt());

        if (complaint.getCitizen() != null) {
            if (maskCitizenInfo) {
                dto.setCitizenName("Verified Citizen");
                dto.setCitizenEmail(null);
                dto.setCitizenPhone(null);
            } else {
                dto.setCitizenName(complaint.getCitizen().getName());
                dto.setCitizenEmail(complaint.getCitizen().getEmail());
                dto.setCitizenPhone(complaint.getCitizen().getPhone());
            }
        }

        if (complaint.getDepartment() != null) {
            dto.setDepartmentId(complaint.getDepartment().getId());
            dto.setDepartmentName(complaint.getDepartment().getName());
        }

        if (complaint.getAssignedOfficer() != null) {
            dto.setAssignedOfficerId(complaint.getAssignedOfficer().getId());
            dto.setAssignedOfficerName(complaint.getAssignedOfficer().getName());
        }

        if (complaint.getImages() != null) {
            dto.setImages(complaint.getImages().stream()
                    .map(img -> new ImageDto(img.getId(), img.getFilePath(), img.getFileName()))
                    .collect(Collectors.toList()));
        }

        if (complaint.getUpdates() != null) {
            dto.setUpdates(complaint.getUpdates().stream()
                    .map(up -> new UpdateDto(
                            up.getId(),
                            up.getStatus(),
                            up.getMessage(),
                            up.getUpdatedBy() != null ? up.getUpdatedBy().getName() : "System",
                            up.getProofImagePath(),
                            up.getCreatedAt()))
                    .collect(Collectors.toList()));
        }

        return dto;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getComplaintId() { return complaintId; }
    public void setComplaintId(String complaintId) { this.complaintId = complaintId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public ComplaintPriority getPriority() { return priority; }
    public void setPriority(ComplaintPriority priority) { this.priority = priority; }

    public ComplaintStatus getStatus() { return status; }
    public void setStatus(ComplaintStatus status) { this.status = status; }

    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getCitizenName() { return citizenName; }
    public void setCitizenName(String citizenName) { this.citizenName = citizenName; }

    public String getCitizenEmail() { return citizenEmail; }
    public void setCitizenEmail(String citizenEmail) { this.citizenEmail = citizenEmail; }

    public String getCitizenPhone() { return citizenPhone; }
    public void setCitizenPhone(String citizenPhone) { this.citizenPhone = citizenPhone; }

    public Long getDepartmentId() { return departmentId; }
    public void setDepartmentId(Long departmentId) { this.departmentId = departmentId; }

    public String getDepartmentName() { return departmentName; }
    public void setDepartmentName(String departmentName) { this.departmentName = departmentName; }

    public Long getAssignedOfficerId() { return assignedOfficerId; }
    public void setAssignedOfficerId(Long assignedOfficerId) { this.assignedOfficerId = assignedOfficerId; }

    public String getAssignedOfficerName() { return assignedOfficerName; }
    public void setAssignedOfficerName(String assignedOfficerName) { this.assignedOfficerName = assignedOfficerName; }

    public int getUpvoteCount() { return upvoteCount; }
    public void setUpvoteCount(int upvoteCount) { this.upvoteCount = upvoteCount; }

    public boolean isHasUpvoted() { return hasUpvoted; }
    public void setHasUpvoted(boolean hasUpvoted) { this.hasUpvoted = hasUpvoted; }

    public List<ImageDto> getImages() { return images; }
    public void setImages(List<ImageDto> images) { this.images = images; }

    public List<UpdateDto> getUpdates() { return updates; }
    public void setUpdates(List<UpdateDto> updates) { this.updates = updates; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
