package com.civicfix.service;

import com.civicfix.dto.*;
import com.civicfix.entity.*;
import com.civicfix.exception.BadRequestException;
import com.civicfix.exception.ResourceNotFoundException;
import com.civicfix.repository.*;
import com.civicfix.util.ComplaintIdGenerator;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ComplaintService {

    private final ComplaintRepository complaintRepository;
    private final ComplaintImageRepository complaintImageRepository;
    private final ComplaintUpdateRepository complaintUpdateRepository;
    private final ComplaintUpvoteRepository complaintUpvoteRepository;
    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;
    private final ComplaintIdGenerator idGenerator;
    private final FileStorageService fileStorageService;
    private final NotificationService notificationService;

    public ComplaintService(
            ComplaintRepository complaintRepository,
            ComplaintImageRepository complaintImageRepository,
            ComplaintUpdateRepository complaintUpdateRepository,
            ComplaintUpvoteRepository complaintUpvoteRepository,
            DepartmentRepository departmentRepository,
            UserRepository userRepository,
            ComplaintIdGenerator idGenerator,
            FileStorageService fileStorageService,
            NotificationService notificationService) {
        this.complaintRepository = complaintRepository;
        this.complaintImageRepository = complaintImageRepository;
        this.complaintUpdateRepository = complaintUpdateRepository;
        this.complaintUpvoteRepository = complaintUpvoteRepository;
        this.departmentRepository = departmentRepository;
        this.userRepository = userRepository;
        this.idGenerator = idGenerator;
        this.fileStorageService = fileStorageService;
        this.notificationService = notificationService;
    }

    @Transactional
    public ComplaintResponseDto createComplaint(ComplaintCreateDto dto, List<MultipartFile> files, User citizen) {
        Complaint complaint = new Complaint();
        complaint.setComplaintId(idGenerator.generateId());
        complaint.setTitle(dto.getTitle().trim());
        complaint.setDescription(dto.getDescription().trim());
        complaint.setCategory(dto.getCategory().trim());
        complaint.setPriority(dto.getPriority() != null ? dto.getPriority() : ComplaintPriority.MEDIUM);
        complaint.setStatus(ComplaintStatus.SUBMITTED);
        complaint.setLatitude(dto.getLatitude());
        complaint.setLongitude(dto.getLongitude());
        complaint.setAddress(dto.getAddress().trim());
        complaint.setCitizen(citizen);

        Complaint savedComplaint = complaintRepository.save(complaint);

        // Store attachments
        if (files != null && !files.isEmpty()) {
            for (MultipartFile file : files) {
                if (file != null && !file.isEmpty()) {
                    String filePath = fileStorageService.storeFile(file);
                    ComplaintImage image = new ComplaintImage(
                            savedComplaint,
                            filePath,
                            file.getOriginalFilename() != null ? file.getOriginalFilename() : "image.jpg",
                            file.getContentType() != null ? file.getContentType() : "image/jpeg"
                    );
                    savedComplaint.addImage(image);
                    complaintImageRepository.save(image);
                }
            }
        }

        // Add Initial Audit / Timeline Entry
        ComplaintUpdate initialUpdate = new ComplaintUpdate(
                savedComplaint,
                ComplaintStatus.SUBMITTED,
                "Complaint successfully registered into the CivicFix portal.",
                citizen
        );
        savedComplaint.addUpdate(initialUpdate);
        complaintUpdateRepository.save(initialUpdate);

        // Send In-App Notification
        notificationService.notifyUser(
                citizen,
                savedComplaint,
                String.format("Your complaint %s (%s) has been submitted successfully.", savedComplaint.getComplaintId(), savedComplaint.getTitle()),
                "SUBMITTED"
        );

        return ComplaintResponseDto.fromEntity(savedComplaint, false, false);
    }

    @Transactional(readOnly = true)
    public ComplaintResponseDto getComplaintById(Long id, User currentUser) {
        Complaint complaint = complaintRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Complaint not found with ID: " + id));

        boolean isOwnerOrStaff = isOwnerOrStaff(complaint, currentUser);
        boolean hasUpvoted = currentUser != null && complaintUpvoteRepository.existsByUserIdAndComplaintId(currentUser.getId(), complaint.getId());

        return ComplaintResponseDto.fromEntity(complaint, !isOwnerOrStaff, hasUpvoted);
    }

    @Transactional(readOnly = true)
    public ComplaintResponseDto getComplaintByCode(String complaintId, User currentUser) {
        Complaint complaint = complaintRepository.findByComplaintId(complaintId.trim().toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("No complaint found with ID: " + complaintId));

        boolean isOwnerOrStaff = isOwnerOrStaff(complaint, currentUser);
        boolean hasUpvoted = currentUser != null && complaintUpvoteRepository.existsByUserIdAndComplaintId(currentUser.getId(), complaint.getId());

        return ComplaintResponseDto.fromEntity(complaint, !isOwnerOrStaff, hasUpvoted);
    }

    @Transactional(readOnly = true)
    public Page<ComplaintResponseDto> getPublicComplaints(
            String category,
            ComplaintStatus status,
            ComplaintPriority priority,
            Long departmentId,
            String keyword,
            Pageable pageable,
            User currentUser) {

        Page<Complaint> page = complaintRepository.searchComplaints(
                category, status, priority, departmentId, keyword, pageable
        );

        Set<Long> upvotedComplaintIds = Collections.emptySet();
        if (currentUser != null) {
            upvotedComplaintIds = page.getContent().stream()
                    .filter(c -> complaintUpvoteRepository.existsByUserIdAndComplaintId(currentUser.getId(), c.getId()))
                    .map(Complaint::getId)
                    .collect(Collectors.toSet());
        }

        final Set<Long> finalUpvotedIds = upvotedComplaintIds;
        List<ComplaintResponseDto> dtos = page.getContent().stream()
                .map(c -> {
                    boolean isOwnerOrStaff = isOwnerOrStaff(c, currentUser);
                    return ComplaintResponseDto.fromEntity(c, !isOwnerOrStaff, finalUpvotedIds.contains(c.getId()));
                })
                .collect(Collectors.toList());

        return new PageImpl<>(dtos, pageable, page.getTotalElements());
    }

    @Transactional(readOnly = true)
    public List<ComplaintResponseDto> getCitizenComplaints(Long citizenId) {
        List<Complaint> complaints = complaintRepository.findByCitizenIdOrderByCreatedAtDesc(citizenId);
        return complaints.stream()
                .map(c -> {
                    boolean hasUpvoted = complaintUpvoteRepository.existsByUserIdAndComplaintId(citizenId, c.getId());
                    return ComplaintResponseDto.fromEntity(c, false, hasUpvoted);
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ComplaintResponseDto> getOfficerComplaints(Long officerId) {
        List<Complaint> complaints = complaintRepository.findByAssignedOfficerIdOrderByCreatedAtDesc(officerId);
        return complaints.stream()
                .map(c -> ComplaintResponseDto.fromEntity(c, false, false))
                .collect(Collectors.toList());
    }

    @Transactional
    public ComplaintResponseDto updateComplaintStatus(Long complaintId, StatusUpdateDto dto, User updatedBy) {
        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new ResourceNotFoundException("Complaint not found with ID: " + complaintId));

        ComplaintStatus oldStatus = complaint.getStatus();
        complaint.setStatus(dto.getStatus());

        if (dto.getStatus() == ComplaintStatus.REJECTED) {
            complaint.setRejectionReason(dto.getRejectionReason() != null ? dto.getRejectionReason() : dto.getNote());
        }

        Complaint savedComplaint = complaintRepository.save(complaint);

        String message = dto.getNote() != null && !dto.getNote().isBlank()
                ? dto.getNote()
                : String.format("Status updated from %s to %s", oldStatus.getDisplayName(), dto.getStatus().getDisplayName());

        if (dto.getStatus() == ComplaintStatus.REJECTED && dto.getRejectionReason() != null) {
            message = "Complaint rejected: " + dto.getRejectionReason();
        }

        ComplaintUpdate update = new ComplaintUpdate(savedComplaint, dto.getStatus(), message, updatedBy);
        complaintUpdateRepository.save(update);
        savedComplaint.addUpdate(update);

        // Notify Citizen
        notificationService.notifyUser(
                savedComplaint.getCitizen(),
                savedComplaint,
                String.format("Update on %s: Status changed to '%s'. %s",
                        savedComplaint.getComplaintId(),
                        dto.getStatus().getDisplayName(),
                        dto.getNote() != null ? dto.getNote() : ""),
                dto.getStatus().name()
        );

        return ComplaintResponseDto.fromEntity(savedComplaint, false, false);
    }

    @Transactional
    public ComplaintResponseDto assignComplaint(Long complaintId, AssignComplaintDto dto, User assignedBy) {
        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new ResourceNotFoundException("Complaint not found with ID: " + complaintId));

        Department department = departmentRepository.findById(dto.getDepartmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with ID: " + dto.getDepartmentId()));
        complaint.setDepartment(department);

        User officer = null;
        if (dto.getOfficerId() != null) {
            officer = userRepository.findById(dto.getOfficerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Officer not found with ID: " + dto.getOfficerId()));
            complaint.setAssignedOfficer(officer);
        }

        // If status was Submitted/Under Review/Verified, promote to ASSIGNED
        if (complaint.getStatus() == ComplaintStatus.SUBMITTED ||
            complaint.getStatus() == ComplaintStatus.UNDER_REVIEW ||
            complaint.getStatus() == ComplaintStatus.VERIFIED) {
            complaint.setStatus(ComplaintStatus.ASSIGNED);
        }

        Complaint savedComplaint = complaintRepository.save(complaint);

        String assignMsg = String.format("Assigned to %s%s.%s",
                department.getName(),
                officer != null ? " (Officer: " + officer.getName() + ")" : "",
                dto.getNote() != null && !dto.getNote().isBlank() ? " Note: " + dto.getNote() : "");

        ComplaintUpdate update = new ComplaintUpdate(savedComplaint, savedComplaint.getStatus(), assignMsg, assignedBy);
        complaintUpdateRepository.save(update);
        savedComplaint.addUpdate(update);

        // Notify Citizen
        notificationService.notifyUser(
                savedComplaint.getCitizen(),
                savedComplaint,
                String.format("Your complaint %s has been assigned to %s.", savedComplaint.getComplaintId(), department.getName()),
                "ASSIGNED"
        );

        // Notify Officer if assigned
        if (officer != null) {
            notificationService.notifyUser(
                    officer,
                    savedComplaint,
                    String.format("You have been assigned new complaint %s: %s", savedComplaint.getComplaintId(), savedComplaint.getTitle()),
                    "ASSIGNED"
            );
        }

        return ComplaintResponseDto.fromEntity(savedComplaint, false, false);
    }

    @Transactional
    public ComplaintResponseDto addProgressUpdate(Long complaintId, ComplaintUpdateDto dto, MultipartFile proofImage, User updatedBy) {
        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new ResourceNotFoundException("Complaint not found with ID: " + complaintId));

        String proofImagePath = null;
        if (proofImage != null && !proofImage.isEmpty()) {
            proofImagePath = fileStorageService.storeFile(proofImage);
        } else if (dto.getProofImagePath() != null) {
            proofImagePath = dto.getProofImagePath();
        }

        if (dto.getStatus() != null) {
            complaint.setStatus(dto.getStatus());
        }

        Complaint savedComplaint = complaintRepository.save(complaint);

        ComplaintUpdate update = new ComplaintUpdate(
                savedComplaint,
                savedComplaint.getStatus(),
                dto.getMessage(),
                updatedBy,
                proofImagePath
        );
        complaintUpdateRepository.save(update);
        savedComplaint.addUpdate(update);

        // Notify Citizen
        notificationService.notifyUser(
                savedComplaint.getCitizen(),
                savedComplaint,
                String.format("Field update on %s (%s): %s",
                        savedComplaint.getComplaintId(),
                        savedComplaint.getStatus().getDisplayName(),
                        dto.getMessage()),
                savedComplaint.getStatus().name()
        );

        return ComplaintResponseDto.fromEntity(savedComplaint, false, false);
    }

    @Transactional
    public Map<String, Object> toggleUpvote(Long complaintId, User user) {
        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new ResourceNotFoundException("Complaint not found with ID: " + complaintId));

        Optional<ComplaintUpvote> existing = complaintUpvoteRepository.findByUserIdAndComplaintId(user.getId(), complaint.getId());
        boolean hasUpvoted;

        if (existing.isPresent()) {
            complaintUpvoteRepository.delete(existing.get());
            complaint.setUpvoteCount(Math.max(0, complaint.getUpvoteCount() - 1));
            hasUpvoted = false;
        } else {
            ComplaintUpvote upvote = new ComplaintUpvote(user, complaint);
            complaintUpvoteRepository.save(upvote);
            complaint.setUpvoteCount(complaint.getUpvoteCount() + 1);
            hasUpvoted = true;
        }

        complaintRepository.save(complaint);

        Map<String, Object> response = new HashMap<>();
        response.put("complaintId", complaint.getComplaintId());
        response.put("upvoteCount", complaint.getUpvoteCount());
        response.put("hasUpvoted", hasUpvoted);
        return response;
    }

    @Transactional(readOnly = true)
    public AdminStatisticsDto getAdminStatistics() {
        AdminStatisticsDto dto = new AdminStatisticsDto();

        dto.setTotalComplaints(complaintRepository.count());
        dto.setNewComplaints(complaintRepository.countByStatus(ComplaintStatus.SUBMITTED));
        dto.setUnderReviewComplaints(complaintRepository.countByStatus(ComplaintStatus.UNDER_REVIEW));
        dto.setInProgressComplaints(complaintRepository.countByStatus(ComplaintStatus.IN_PROGRESS) +
                                    complaintRepository.countByStatus(ComplaintStatus.ASSIGNED) +
                                    complaintRepository.countByStatus(ComplaintStatus.VERIFIED));
        dto.setResolvedComplaints(complaintRepository.countByStatus(ComplaintStatus.RESOLVED));
        dto.setRejectedComplaints(complaintRepository.countByStatus(ComplaintStatus.REJECTED));
        dto.setClosedComplaints(complaintRepository.countByStatus(ComplaintStatus.CLOSED));

        // Category breakdown
        Map<String, Long> byCategory = new HashMap<>();
        for (Object[] row : complaintRepository.countComplaintsByCategory()) {
            byCategory.put((String) row[0], ((Number) row[1]).longValue());
        }
        dto.setComplaintsByCategory(byCategory);

        // Status breakdown
        Map<String, Long> byStatus = new HashMap<>();
        for (Object[] row : complaintRepository.countComplaintsByStatus()) {
            byStatus.put(((ComplaintStatus) row[0]).name(), ((Number) row[1]).longValue());
        }
        dto.setComplaintsByStatus(byStatus);

        // Priority breakdown
        Map<String, Long> byPriority = new HashMap<>();
        for (Object[] row : complaintRepository.countComplaintsByPriority()) {
            byPriority.put(((ComplaintPriority) row[0]).name(), ((Number) row[1]).longValue());
        }
        dto.setComplaintsByPriority(byPriority);

        // Top recent
        List<Complaint> recent = complaintRepository.findTop100ByOrderByCreatedAtDesc();
        dto.setRecentComplaints(recent.stream()
                .map(c -> ComplaintResponseDto.fromEntity(c, false, false))
                .collect(Collectors.toList()));

        return dto;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getPublicSummaryStatistics() {
        long total = complaintRepository.count();
        long resolved = complaintRepository.countByStatus(ComplaintStatus.RESOLVED) + complaintRepository.countByStatus(ComplaintStatus.CLOSED);
        long inProgress = complaintRepository.countByStatus(ComplaintStatus.IN_PROGRESS) + complaintRepository.countByStatus(ComplaintStatus.ASSIGNED);
        long active = total - resolved - complaintRepository.countByStatus(ComplaintStatus.REJECTED);

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalReported", total);
        stats.put("resolved", resolved);
        stats.put("inProgress", inProgress);
        stats.put("activeIssues", Math.max(0, active));
        return stats;
    }

    private boolean isOwnerOrStaff(Complaint complaint, User currentUser) {
        if (currentUser == null) return false;
        if (currentUser.getRole() == Role.ROLE_ADMIN) return true;
        if (currentUser.getRole() == Role.ROLE_OFFICER &&
                complaint.getAssignedOfficer() != null &&
                complaint.getAssignedOfficer().getId().equals(currentUser.getId())) {
            return true;
        }
        return complaint.getCitizen() != null && complaint.getCitizen().getId().equals(currentUser.getId());
    }
}
