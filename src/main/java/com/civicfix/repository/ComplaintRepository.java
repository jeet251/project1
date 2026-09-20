package com.civicfix.repository;

import com.civicfix.entity.Complaint;
import com.civicfix.entity.ComplaintPriority;
import com.civicfix.entity.ComplaintStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ComplaintRepository extends JpaRepository<Complaint, Long> {

    Optional<Complaint> findByComplaintId(String complaintId);

    List<Complaint> findByCitizenIdOrderByCreatedAtDesc(Long citizenId);

    List<Complaint> findByAssignedOfficerIdOrderByCreatedAtDesc(Long officerId);

    List<Complaint> findByDepartmentIdOrderByCreatedAtDesc(Long departmentId);

    // Active complaints by category for duplicate detection
    @Query("SELECT c FROM Complaint c WHERE c.category = :category AND c.status NOT IN ('RESOLVED', 'CLOSED', 'REJECTED')")
    List<Complaint> findActiveByCategory(@Param("category") String category);

    // Advanced search & filter query for public / admin explorer
    @Query("SELECT c FROM Complaint c WHERE " +
           "(:category IS NULL OR c.category = :category) AND " +
           "(:status IS NULL OR c.status = :status) AND " +
           "(:priority IS NULL OR c.priority = :priority) AND " +
           "(:departmentId IS NULL OR c.department.id = :departmentId) AND " +
           "(:keyword IS NULL OR LOWER(c.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(c.description) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(c.address) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(c.complaintId) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Complaint> searchComplaints(
            @Param("category") String category,
            @Param("status") ComplaintStatus status,
            @Param("priority") ComplaintPriority priority,
            @Param("departmentId") Long departmentId,
            @Param("keyword") String keyword,
            Pageable pageable);

    // Statistics counts
    long countByStatus(ComplaintStatus status);

    @Query("SELECT c.category, COUNT(c) FROM Complaint c GROUP BY c.category")
    List<Object[]> countComplaintsByCategory();

    @Query("SELECT c.status, COUNT(c) FROM Complaint c GROUP BY c.status")
    List<Object[]> countComplaintsByStatus();

    @Query("SELECT c.priority, COUNT(c) FROM Complaint c GROUP BY c.priority")
    List<Object[]> countComplaintsByPriority();

    // Citizen counts
    long countByCitizenId(Long citizenId);
    long countByCitizenIdAndStatus(Long citizenId, ComplaintStatus status);

    // Officer counts
    long countByAssignedOfficerId(Long officerId);
    long countByAssignedOfficerIdAndStatus(Long officerId, ComplaintStatus status);

    // Recent complaints for timeline chart
    List<Complaint> findTop100ByOrderByCreatedAtDesc();
}
