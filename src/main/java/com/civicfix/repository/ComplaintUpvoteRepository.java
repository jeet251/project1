package com.civicfix.repository;

import com.civicfix.entity.ComplaintUpvote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ComplaintUpvoteRepository extends JpaRepository<ComplaintUpvote, Long> {
    Optional<ComplaintUpvote> findByUserIdAndComplaintId(Long userId, Long complaintId);
    boolean existsByUserIdAndComplaintId(Long userId, Long complaintId);
    void deleteByUserIdAndComplaintId(Long userId, Long complaintId);
}
