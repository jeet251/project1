package com.civicfix.controller;

import com.civicfix.dto.*;
import com.civicfix.entity.ComplaintPriority;
import com.civicfix.entity.ComplaintStatus;
import com.civicfix.entity.User;
import com.civicfix.service.ComplaintService;
import com.civicfix.service.DuplicateDetectionService;
import com.civicfix.service.UserService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ComplaintController {

    private final ComplaintService complaintService;
    private final UserService userService;
    private final DuplicateDetectionService duplicateDetectionService;

    public ComplaintController(ComplaintService complaintService,
                               UserService userService,
                               DuplicateDetectionService duplicateDetectionService) {
        this.complaintService = complaintService;
        this.userService = userService;
        this.duplicateDetectionService = duplicateDetectionService;
    }

    /**
     * Submit a new complaint with multipart file evidence
     */
    @PostMapping(value = "/complaints", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<ComplaintResponseDto> createComplaintMultipart(
            @RequestParam("title") String title,
            @RequestParam("description") String description,
            @RequestParam("category") String category,
            @RequestParam(value = "priority", defaultValue = "MEDIUM") ComplaintPriority priority,
            @RequestParam("latitude") Double latitude,
            @RequestParam("longitude") Double longitude,
            @RequestParam("address") String address,
            @RequestParam(value = "files", required = false) List<MultipartFile> files,
            @AuthenticationPrincipal UserDetails userDetails) {

        User citizen = userService.getUserByEmail(userDetails.getUsername());

        ComplaintCreateDto dto = new ComplaintCreateDto();
        dto.setTitle(title);
        dto.setDescription(description);
        dto.setCategory(category);
        dto.setPriority(priority);
        dto.setLatitude(latitude);
        dto.setLongitude(longitude);
        dto.setAddress(address);

        ComplaintResponseDto response = complaintService.createComplaint(dto, files, citizen);
        return ResponseEntity.ok(response);
    }

    /**
     * Optional JSON complaint creation (e.g. without attachments or programmatic API)
     */
    @PostMapping(value = "/complaints/json", consumes = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<ComplaintResponseDto> createComplaintJson(
            @Valid @RequestBody ComplaintCreateDto dto,
            @AuthenticationPrincipal UserDetails userDetails) {

        User citizen = userService.getUserByEmail(userDetails.getUsername());
        ComplaintResponseDto response = complaintService.createComplaint(dto, null, citizen);
        return ResponseEntity.ok(response);
    }

    /**
     * Check for nearby duplicate issues in the same category
     */
    @PostMapping("/complaints/check-duplicate")
    public ResponseEntity<List<DuplicateIssueDto>> checkDuplicates(@Valid @RequestBody DuplicateCheckDto request) {
        List<DuplicateIssueDto> duplicates = duplicateDetectionService.findNearbySimilarIssues(
                request.getCategory(),
                request.getLatitude(),
                request.getLongitude(),
                request.getRadiusMeters() != null ? request.getRadiusMeters() : 100.0
        );
        return ResponseEntity.ok(duplicates);
    }

    /**
     * Public explorer feed with filters
     */
    @GetMapping("/complaints/public")
    public ResponseEntity<Page<ComplaintResponseDto>> getPublicComplaints(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) ComplaintStatus status,
            @RequestParam(required = false) ComplaintPriority priority,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction,
            @AuthenticationPrincipal UserDetails userDetails) {

        User currentUser = userDetails != null ? userService.getUserByEmail(userDetails.getUsername()) : null;
        Sort sort = direction.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        PageRequest pageRequest = PageRequest.of(page, size, sort);

        Page<ComplaintResponseDto> result = complaintService.getPublicComplaints(
                category, status, priority, departmentId, keyword, pageRequest, currentUser
        );
        return ResponseEntity.ok(result);
    }

    /**
     * Citizen's personal complaints list
     */
    @GetMapping("/complaints/my")
    public ResponseEntity<List<ComplaintResponseDto>> getMyComplaints(@AuthenticationPrincipal UserDetails userDetails) {
        User citizen = userService.getUserByEmail(userDetails.getUsername());
        List<ComplaintResponseDto> complaints = complaintService.getCitizenComplaints(citizen.getId());
        return ResponseEntity.ok(complaints);
    }

    /**
     * Public tracking by unique Complaint ID (e.g. CIV-2026-00001)
     */
    @GetMapping("/complaints/by-code/{complaintId}")
    public ResponseEntity<ComplaintResponseDto> getComplaintByCode(
            @PathVariable String complaintId,
            @AuthenticationPrincipal UserDetails userDetails) {
        User currentUser = userDetails != null ? userService.getUserByEmail(userDetails.getUsername()) : null;
        ComplaintResponseDto complaint = complaintService.getComplaintByCode(complaintId, currentUser);
        return ResponseEntity.ok(complaint);
    }

    /**
     * Get complaint details by database ID
     */
    @GetMapping("/complaints/{id}")
    public ResponseEntity<ComplaintResponseDto> getComplaintById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        User currentUser = userDetails != null ? userService.getUserByEmail(userDetails.getUsername()) : null;
        ComplaintResponseDto complaint = complaintService.getComplaintById(id, currentUser);
        return ResponseEntity.ok(complaint);
    }

    /**
     * Citizen upvote / un-upvote toggle
     */
    @PostMapping("/complaints/{id}/upvote")
    public ResponseEntity<Map<String, Object>> toggleUpvote(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        User currentUser = userService.getUserByEmail(userDetails.getUsername());
        Map<String, Object> result = complaintService.toggleUpvote(id, currentUser);
        return ResponseEntity.ok(result);
    }

    /**
     * Public summary statistics for the landing page hero counter
     */
    @GetMapping("/statistics/summary")
    public ResponseEntity<Map<String, Object>> getSummaryStats() {
        return ResponseEntity.ok(complaintService.getPublicSummaryStatistics());
    }
}
