package com.civicfix.controller;

import com.civicfix.dto.*;
import com.civicfix.entity.ComplaintPriority;
import com.civicfix.entity.ComplaintStatus;
import com.civicfix.entity.User;
import com.civicfix.service.ComplaintService;
import com.civicfix.service.UserService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final ComplaintService complaintService;
    private final UserService userService;

    public AdminController(ComplaintService complaintService, UserService userService) {
        this.complaintService = complaintService;
        this.userService = userService;
    }

    /**
     * Complete admin view of all complaints with filtering
     */
    @GetMapping("/complaints")
    public ResponseEntity<Page<ComplaintResponseDto>> getAllComplaints(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) ComplaintStatus status,
            @RequestParam(required = false) ComplaintPriority priority,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction,
            @AuthenticationPrincipal UserDetails userDetails) {

        User admin = userService.getUserByEmail(userDetails.getUsername());
        Sort sort = direction.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        PageRequest pageRequest = PageRequest.of(page, size, sort);

        Page<ComplaintResponseDto> result = complaintService.getPublicComplaints(
                category, status, priority, departmentId, keyword, pageRequest, admin
        );
        return ResponseEntity.ok(result);
    }

    /**
     * Change complaint status (Verify, Reject, Close, etc.)
     */
    @PutMapping("/complaints/{id}/status")
    public ResponseEntity<ComplaintResponseDto> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody StatusUpdateDto dto,
            @AuthenticationPrincipal UserDetails userDetails) {

        User admin = userService.getUserByEmail(userDetails.getUsername());
        ComplaintResponseDto updated = complaintService.updateComplaintStatus(id, dto, admin);
        return ResponseEntity.ok(updated);
    }

    /**
     * Assign complaint to department and designated officer
     */
    @PutMapping("/complaints/{id}/assign")
    public ResponseEntity<ComplaintResponseDto> assignComplaint(
            @PathVariable Long id,
            @Valid @RequestBody AssignComplaintDto dto,
            @AuthenticationPrincipal UserDetails userDetails) {

        User admin = userService.getUserByEmail(userDetails.getUsername());
        ComplaintResponseDto updated = complaintService.assignComplaint(id, dto, admin);
        return ResponseEntity.ok(updated);
    }

    /**
     * Executive analytics and charts data
     */
    @GetMapping("/statistics")
    public ResponseEntity<AdminStatisticsDto> getStatistics() {
        return ResponseEntity.ok(complaintService.getAdminStatistics());
    }

    /**
     * List officers for assignment dropdown
     */
    @GetMapping("/officers")
    public ResponseEntity<List<UserDto>> getOfficers(@RequestParam(required = false) Long departmentId) {
        return ResponseEntity.ok(userService.getOfficers(departmentId));
    }
}
