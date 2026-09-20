package com.civicfix.dto;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdminStatisticsDto {

    private long totalComplaints;
    private long newComplaints;
    private long underReviewComplaints;
    private long inProgressComplaints;
    private long resolvedComplaints;
    private long rejectedComplaints;
    private long closedComplaints;

    private Map<String, Long> complaintsByCategory = new HashMap<>();
    private Map<String, Long> complaintsByStatus = new HashMap<>();
    private Map<String, Long> complaintsByPriority = new HashMap<>();

    private List<ComplaintResponseDto> recentComplaints;

    public AdminStatisticsDto() {
    }

    public long getTotalComplaints() {
        return totalComplaints;
    }

    public void setTotalComplaints(long totalComplaints) {
        this.totalComplaints = totalComplaints;
    }

    public long getNewComplaints() {
        return newComplaints;
    }

    public void setNewComplaints(long newComplaints) {
        this.newComplaints = newComplaints;
    }

    public long getUnderReviewComplaints() {
        return underReviewComplaints;
    }

    public void setUnderReviewComplaints(long underReviewComplaints) {
        this.underReviewComplaints = underReviewComplaints;
    }

    public long getInProgressComplaints() {
        return inProgressComplaints;
    }

    public void setInProgressComplaints(long inProgressComplaints) {
        this.inProgressComplaints = inProgressComplaints;
    }

    public long getResolvedComplaints() {
        return resolvedComplaints;
    }

    public void setResolvedComplaints(long resolvedComplaints) {
        this.resolvedComplaints = resolvedComplaints;
    }

    public long getRejectedComplaints() {
        return rejectedComplaints;
    }

    public void setRejectedComplaints(long rejectedComplaints) {
        this.rejectedComplaints = rejectedComplaints;
    }

    public long getClosedComplaints() {
        return closedComplaints;
    }

    public void setClosedComplaints(long closedComplaints) {
        this.closedComplaints = closedComplaints;
    }

    public Map<String, Long> getComplaintsByCategory() {
        return complaintsByCategory;
    }

    public void setComplaintsByCategory(Map<String, Long> complaintsByCategory) {
        this.complaintsByCategory = complaintsByCategory;
    }

    public Map<String, Long> getComplaintsByStatus() {
        return complaintsByStatus;
    }

    public void setComplaintsByStatus(Map<String, Long> complaintsByStatus) {
        this.complaintsByStatus = complaintsByStatus;
    }

    public Map<String, Long> getComplaintsByPriority() {
        return complaintsByPriority;
    }

    public void setComplaintsByPriority(Map<String, Long> complaintsByPriority) {
        this.complaintsByPriority = complaintsByPriority;
    }

    public List<ComplaintResponseDto> getRecentComplaints() {
        return recentComplaints;
    }

    public void setRecentComplaints(List<ComplaintResponseDto> recentComplaints) {
        this.recentComplaints = recentComplaints;
    }
}
