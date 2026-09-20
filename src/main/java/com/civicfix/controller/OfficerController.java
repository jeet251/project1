package com.civicfix.controller;

import com.civicfix.dto.ComplaintResponseDto;
import com.civicfix.dto.ComplaintUpdateDto;
import com.civicfix.dto.StatusUpdateDto;
import com.civicfix.entity.ComplaintStatus;
import com.civicfix.entity.User;
import com.civicfix.service.ComplaintService;
import com.civicfix.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/officer")
@PreAuthorize("hasAnyRole('OFFICER', 'ADMIN')")
public class OfficerController {

    private final ComplaintService complaintService;
    private final UserService userService;

    public OfficerController(ComplaintService complaintService, UserService userService) {
        this.complaintService = complaintService;
        this.userService = userService;
    }

    /**
     * Get complaints assigned specifically to the logged-in officer
     */
    @GetMapping("/complaints")
    public ResponseEntity<List<ComplaintResponseDto>> getAssignedComplaints(@AuthenticationPrincipal UserDetails userDetails) {
        User officer = userService.getUserByEmail(userDetails.getUsername());
        List<ComplaintResponseDto> complaints = complaintService.getOfficerComplaints(officer.getId());
        return ResponseEntity.ok(complaints);
    }

    /**
     * Post a progress update note and optional completion/progress proof image
     */
    @PostMapping(value = "/complaints/{id}/updates", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<ComplaintResponseDto> addProgressUpdate(
            @PathVariable Long id,
            @RequestParam("status") ComplaintStatus status,
            @RequestParam("message") String message,
            @RequestParam(value = "proofImage", required = false) MultipartFile proofImage,
            @AuthenticationPrincipal UserDetails userDetails) {

        User officer = userService.getUserByEmail(userDetails.getUsername());
        ComplaintUpdateDto dto = new ComplaintUpdateDto(status, message);
        ComplaintResponseDto response = complaintService.addProgressUpdate(id, dto, proofImage, officer);
        return ResponseEntity.ok(response);
    }

    /**
     * Post JSON progress update
     */
    @PostMapping(value = "/complaints/{id}/updates/json", consumes = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<ComplaintResponseDto> addProgressUpdateJson(
            @PathVariable Long id,
            @Valid @RequestBody ComplaintUpdateDto dto,
            @AuthenticationPrincipal UserDetails userDetails) {

        User officer = userService.getUserByEmail(userDetails.getUsername());
        ComplaintResponseDto response = complaintService.addProgressUpdate(id, dto, null, officer);
        return ResponseEntity.ok(response);
    }

    /**
     * Quick status update by officer (e.g. IN_PROGRESS or RESOLVED)
     */
    @PutMapping("/complaints/{id}/status")
    public ResponseEntity<ComplaintResponseDto> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody StatusUpdateDto dto,
            @AuthenticationPrincipal UserDetails userDetails) {

        User officer = userService.getUserByEmail(userDetails.getUsername());
        ComplaintResponseDto response = complaintService.updateComplaintStatus(id, dto, officer);
        return ResponseEntity.ok(response);
    }
}
