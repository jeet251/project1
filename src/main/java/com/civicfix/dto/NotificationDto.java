package com.civicfix.dto;

import com.civicfix.entity.Notification;
import java.time.LocalDateTime;

public class NotificationDto {

    private Long id;
    private String message;
    private String type;
    private boolean read;
    private LocalDateTime createdAt;
    private String complaintId;
    private String complaintTitle;

    public NotificationDto() {
    }

    public static NotificationDto fromEntity(Notification n) {
        NotificationDto dto = new NotificationDto();
        dto.setId(n.getId());
        dto.setMessage(n.getMessage());
        dto.setType(n.getType());
        dto.setRead(n.isRead());
        dto.setCreatedAt(n.getCreatedAt());
        if (n.getComplaint() != null) {
            dto.setComplaintId(n.getComplaint().getComplaintId());
            dto.setComplaintTitle(n.getComplaint().getTitle());
        }
        return dto;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getComplaintId() { return complaintId; }
    public void setComplaintId(String complaintId) { this.complaintId = complaintId; }

    public String getComplaintTitle() { return complaintTitle; }
    public void setComplaintTitle(String complaintTitle) { this.complaintTitle = complaintTitle; }
}
