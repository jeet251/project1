package com.civicfix.service;

import com.civicfix.dto.DuplicateIssueDto;
import com.civicfix.entity.Complaint;
import com.civicfix.repository.ComplaintRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class DuplicateDetectionService {

    private static final double EARTH_RADIUS_METERS = 6371000.0;
    private final ComplaintRepository complaintRepository;

    public DuplicateDetectionService(ComplaintRepository complaintRepository) {
        this.complaintRepository = complaintRepository;
    }

    @Transactional(readOnly = true)
    public List<DuplicateIssueDto> findNearbySimilarIssues(String category, double latitude, double longitude, double radiusMeters) {
        List<Complaint> candidateComplaints = complaintRepository.findActiveByCategory(category);
        List<DuplicateIssueDto> duplicates = new ArrayList<>();

        for (Complaint complaint : candidateComplaints) {
            if (complaint.getLatitude() != null && complaint.getLongitude() != null) {
                double distance = calculateDistanceMeters(
                        latitude, longitude,
                        complaint.getLatitude(), complaint.getLongitude()
                );

                if (distance <= radiusMeters) {
                    duplicates.add(DuplicateIssueDto.fromEntity(complaint, distance));
                }
            }
        }

        duplicates.sort(Comparator.comparingDouble(DuplicateIssueDto::getDistanceMeters));
        return duplicates;
    }

    /**
     * Haversine formula to compute great-circle distance between two points in meters.
     */
    public double calculateDistanceMeters(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2.0) * Math.sin(dLat / 2.0) +
                   Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                   Math.sin(dLon / 2.0) * Math.sin(dLon / 2.0);

        double c = 2.0 * Math.atan2(Math.sqrt(a), Math.sqrt(1.0 - a));
        return EARTH_RADIUS_METERS * c;
    }
}
