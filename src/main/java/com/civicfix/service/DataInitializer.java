package com.civicfix.service;

import com.civicfix.entity.*;
import com.civicfix.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final ComplaintRepository complaintRepository;
    private final ComplaintImageRepository complaintImageRepository;
    private final ComplaintUpdateRepository complaintUpdateRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository,
                           DepartmentRepository departmentRepository,
                           ComplaintRepository complaintRepository,
                           ComplaintImageRepository complaintImageRepository,
                           ComplaintUpdateRepository complaintUpdateRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
        this.complaintRepository = complaintRepository;
        this.complaintImageRepository = complaintImageRepository;
        this.complaintUpdateRepository = complaintUpdateRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        initDepartments();
        initUsers();
        initComplaints();
    }

    private void initDepartments() {
        if (departmentRepository.count() == 0) {
            List<Department> depts = Arrays.asList(
                    new Department("Road Department", "Maintenance and construction of public roads, flyovers, and pavements."),
                    new Department("Water Department", "Water supply pipelines, water meters, and pressure management."),
                    new Department("Sanitation Department", "Garbage collection, waste management, and public street cleanliness."),
                    new Department("Electrical Department", "Streetlights, public lighting grids, and exposed wires."),
                    new Department("Drainage Department", "Stormwater drains, sewage blockages, and overflow prevention."),
                    new Department("Traffic Department", "Traffic signal automation, signage, and road safety installations."),
                    new Department("General Maintenance", "Public parks, civic benches, tree cutting, and structural repairs.")
            );
            departmentRepository.saveAll(depts);
        }
    }

    private void initUsers() {
        if (userRepository.count() == 0) {
            Department roadDept = departmentRepository.findByName("Road Department").orElse(null);
            Department waterDept = departmentRepository.findByName("Water Department").orElse(null);
            Department sanitationDept = departmentRepository.findByName("Sanitation Department").orElse(null);

            // 1. Admin
            User admin = new User("Municipal Administrator", "admin@civicfix.gov", "+1-555-0100", passwordEncoder.encode("Admin@123"), Role.ROLE_ADMIN);
            userRepository.save(admin);

            // 2. Officers
            User officerRoad = new User("Inspector Rajesh Kumar", "officer.road@civicfix.gov", "+1-555-0101", passwordEncoder.encode("Officer@123"), Role.ROLE_OFFICER);
            officerRoad.setDepartment(roadDept);
            userRepository.save(officerRoad);

            User officerWater = new User("Engineer Anita Desai", "officer.water@civicfix.gov", "+1-555-0102", passwordEncoder.encode("Officer@123"), Role.ROLE_OFFICER);
            officerWater.setDepartment(waterDept);
            userRepository.save(officerWater);

            User officerSanitation = new User("Supervisor Vikram Singh", "officer.sanitation@civicfix.gov", "+1-555-0103", passwordEncoder.encode("Officer@123"), Role.ROLE_OFFICER);
            officerSanitation.setDepartment(sanitationDept);
            userRepository.save(officerSanitation);

            // 3. Citizens
            User citizen1 = new User("Amit Patel", "citizen@example.com", "+1-555-0201", passwordEncoder.encode("Citizen@123"), Role.ROLE_CITIZEN);
            userRepository.save(citizen1);

            User citizen2 = new User("Priya Sharma", "priya.sharma@example.com", "+1-555-0202", passwordEncoder.encode("Citizen@123"), Role.ROLE_CITIZEN);
            userRepository.save(citizen2);
        }
    }

    private void initComplaints() {
        if (complaintRepository.count() == 0) {
            User citizen1 = userRepository.findByEmail("citizen@example.com").orElse(null);
            User citizen2 = userRepository.findByEmail("priya.sharma@example.com").orElse(null);
            User officerRoad = userRepository.findByEmail("officer.road@civicfix.gov").orElse(null);
            User officerWater = userRepository.findByEmail("officer.water@civicfix.gov").orElse(null);
            User officerSanitation = userRepository.findByEmail("officer.sanitation@civicfix.gov").orElse(null);
            User admin = userRepository.findByEmail("admin@civicfix.gov").orElse(null);

            Department roadDept = departmentRepository.findByName("Road Department").orElse(null);
            Department waterDept = departmentRepository.findByName("Water Department").orElse(null);
            Department sanitationDept = departmentRepository.findByName("Sanitation Department").orElse(null);
            Department electricalDept = departmentRepository.findByName("Electrical Department").orElse(null);
            Department drainageDept = departmentRepository.findByName("Drainage Department").orElse(null);

            // 1. Pothole - In Progress
            createSampleComplaint(
                    "CIV-2026-00001",
                    "Deep dangerous pothole near Metro Station Pillar 42",
                    "A massive crater has formed right before the exit turn of the metro station. Vehicles have suffered wheel rim damage and two-wheelers are losing balance in the dark.",
                    "Potholes",
                    ComplaintPriority.CRITICAL,
                    ComplaintStatus.IN_PROGRESS,
                    12.9784, 77.5937,
                    "Near Trinity Metro Station, MG Road, Central District",
                    citizen1, roadDept, officerRoad, 14,
                    Arrays.asList(
                            new StatusUpdateHistory(ComplaintStatus.SUBMITTED, "Complaint registered by citizen with photo evidence.", citizen1, LocalDateTime.now().minusDays(5)),
                            new StatusUpdateHistory(ComplaintStatus.VERIFIED, "Municipal field scout inspected the pothole and confirmed severity.", admin, LocalDateTime.now().minusDays(4)),
                            new StatusUpdateHistory(ComplaintStatus.ASSIGNED, "Assigned to Road Department. Officer Rajesh Kumar assigned to site inspection.", admin, LocalDateTime.now().minusDays(3)),
                            new StatusUpdateHistory(ComplaintStatus.IN_PROGRESS, "Asphalt patch crew and heavy steam roller dispatched. Work underway.", officerRoad, LocalDateTime.now().minusDays(1))
                    )
            );

            // 2. Garbage - Resolved
            createSampleComplaint(
                    "CIV-2026-00002",
                    "Overflowing municipal dumpster and scattered organic waste",
                    "Community garbage bins have not been emptied for 4 days. Waste is spilling onto the pedestrian sidewalk creating a foul odor and stray animal menace.",
                    "Garbage",
                    ComplaintPriority.HIGH,
                    ComplaintStatus.RESOLVED,
                    12.9719, 77.6012,
                    "Corner of 4th Cross and Brigade Road, Shanthi Nagar",
                    citizen2, sanitationDept, officerSanitation, 9,
                    Arrays.asList(
                            new StatusUpdateHistory(ComplaintStatus.SUBMITTED, "Complaint submitted by resident.", citizen2, LocalDateTime.now().minusDays(4)),
                            new StatusUpdateHistory(ComplaintStatus.VERIFIED, "Verified by sanitation monitor.", admin, LocalDateTime.now().minusDays(3)),
                            new StatusUpdateHistory(ComplaintStatus.ASSIGNED, "Assigned to Ward 112 Sanitation Quick Response Team.", admin, LocalDateTime.now().minusDays(3)),
                            new StatusUpdateHistory(ComplaintStatus.IN_PROGRESS, "Sanitation truck dispatched with loader crew.", officerSanitation, LocalDateTime.now().minusDays(2)),
                            new StatusUpdateHistory(ComplaintStatus.RESOLVED, "Bins emptied, surrounding area disinfected and bleached. Site cleared.", officerSanitation, LocalDateTime.now().minusDays(1))
                    )
            );

            // 3. Waterlogging - Under Review
            createSampleComplaint(
                    "CIV-2026-00003",
                    "Severe waterlogging under railway underpass after rainfall",
                    "Over 2 feet of stagnant stormwater accumulated under the railway bridge. Small cars and auto-rickshaws are stalling in the flood.",
                    "Waterlogging",
                    ComplaintPriority.HIGH,
                    ComplaintStatus.UNDER_REVIEW,
                    12.9658, 77.5872,
                    "Lalbagh West Gate Underpass, Basavanagudi",
                    citizen1, drainageDept, null, 21,
                    Arrays.asList(
                            new StatusUpdateHistory(ComplaintStatus.SUBMITTED, "Complaint registered with geo-coordinates.", citizen1, LocalDateTime.now().minusHours(18)),
                            new StatusUpdateHistory(ComplaintStatus.UNDER_REVIEW, "Under technical review by Stormwater Drainage cell.", admin, LocalDateTime.now().minusHours(8))
                    )
            );

            // 4. Streetlight - Verified
            createSampleComplaint(
                    "CIV-2026-00004",
                    "Row of 5 solar streetlights not operating along park perimeter",
                    "The walking trail along the public park has been pitch dark for three consecutive nights. Elderly citizens and joggers are unable to use the path safely.",
                    "Streetlights",
                    ComplaintPriority.MEDIUM,
                    ComplaintStatus.VERIFIED,
                    12.9756, 77.6065,
                    "Cubbon Park Eastern Promenade, High Court Ward",
                    citizen2, electricalDept, null, 7,
                    Arrays.asList(
                            new StatusUpdateHistory(ComplaintStatus.SUBMITTED, "Reported by local runner.", citizen2, LocalDateTime.now().minusDays(2)),
                            new StatusUpdateHistory(ComplaintStatus.VERIFIED, "Electrical substation verified line trip on circuit B-4.", admin, LocalDateTime.now().minusDays(1))
                    )
            );

            // 5. Water Supply - Submitted (New)
            createSampleComplaint(
                    "CIV-2026-00005",
                    "Major main pipeline rupture leaking potable drinking water",
                    "Clean drinking water is gushing out of an underground pipeline fracture near the market intersection, flooding the street and dropping building pressure to zero.",
                    "Water Supply",
                    ComplaintPriority.CRITICAL,
                    ComplaintStatus.SUBMITTED,
                    12.9812, 77.5960,
                    "Commercial Street Junction, Tasker Town",
                    citizen1, waterDept, null, 35,
                    Arrays.asList(
                            new StatusUpdateHistory(ComplaintStatus.SUBMITTED, "Urgent pipeline leak reported by citizen.", citizen1, LocalDateTime.now().minusHours(3))
                    )
            );

            // 6. Damaged Roads - Assigned
            createSampleComplaint(
                    "CIV-2026-00006",
                    "Caved-in asphalt trench following optical fiber cable digging",
                    "Telecom contractors dug a 50-meter trench across the service road and backfilled it with loose gravel without proper bituminous paving.",
                    "Damaged Roads",
                    ComplaintPriority.MEDIUM,
                    ComplaintStatus.ASSIGNED,
                    12.9680, 77.6105,
                    "Hosur Main Road Service Lane, Richmond Town",
                    citizen2, roadDept, officerRoad, 5,
                    Arrays.asList(
                            new StatusUpdateHistory(ComplaintStatus.SUBMITTED, "Complaint submitted with trench details.", citizen2, LocalDateTime.now().minusDays(3)),
                            new StatusUpdateHistory(ComplaintStatus.VERIFIED, "Site validated by infrastructure survey.", admin, LocalDateTime.now().minusDays(2)),
                            new StatusUpdateHistory(ComplaintStatus.ASSIGNED, "Assigned to Road Dept for issuing contractor rectification notice.", admin, LocalDateTime.now().minusDays(1))
                    )
            );

            // 7. Fallen Trees - In Progress
            createSampleComplaint(
                    "CIV-2026-00007",
                    "Large Gulmohar branch snapped across electrical wires and road",
                    "Heavy winds caused a major tree branch to collapse. It is leaning heavily against electrical cables and blocking two lanes of northbound traffic.",
                    "Fallen Trees",
                    ComplaintPriority.CRITICAL,
                    ComplaintStatus.IN_PROGRESS,
                    12.9632, 77.5980,
                    "10th Main Road, Wilson Garden",
                    citizen1, null, officerRoad, 18,
                    Arrays.asList(
                            new StatusUpdateHistory(ComplaintStatus.SUBMITTED, "Emergency hazard reported.", citizen1, LocalDateTime.now().minusHours(12)),
                            new StatusUpdateHistory(ComplaintStatus.VERIFIED, "Quick response assessment dispatched.", admin, LocalDateTime.now().minusHours(10)),
                            new StatusUpdateHistory(ComplaintStatus.ASSIGNED, "Assigned to Forest & Emergency Tree Cutting Cell.", admin, LocalDateTime.now().minusHours(8)),
                            new StatusUpdateHistory(ComplaintStatus.IN_PROGRESS, "Hydraulic crane and chain saw team on site clearing the branch.", officerRoad, LocalDateTime.now().minusHours(2))
                    )
            );

            // 8. Drainage - Closed
            createSampleComplaint(
                    "CIV-2026-00008",
                    "Broken manhole cover in residential school zone",
                    "A cast-iron storm drain cover cracked in half, leaving an open hazard right where primary school students cross every morning.",
                    "Drainage",
                    ComplaintPriority.HIGH,
                    ComplaintStatus.CLOSED,
                    12.9735, 77.5890,
                    "Near St. Joseph High School, Museum Road",
                    citizen2, drainageDept, officerRoad, 28,
                    Arrays.asList(
                            new StatusUpdateHistory(ComplaintStatus.SUBMITTED, "Hazardous manhole reported.", citizen2, LocalDateTime.now().minusDays(7)),
                            new StatusUpdateHistory(ComplaintStatus.VERIFIED, "Safety barricades erected immediately.", admin, LocalDateTime.now().minusDays(6)),
                            new StatusUpdateHistory(ComplaintStatus.ASSIGNED, "Assigned for high-load concrete frame replacement.", admin, LocalDateTime.now().minusDays(5)),
                            new StatusUpdateHistory(ComplaintStatus.IN_PROGRESS, "Cast iron frame and reinforced slab installed.", officerRoad, LocalDateTime.now().minusDays(4)),
                            new StatusUpdateHistory(ComplaintStatus.RESOLVED, "Concrete cured and tested for vehicle load.", officerRoad, LocalDateTime.now().minusDays(2)),
                            new StatusUpdateHistory(ComplaintStatus.CLOSED, "Citizen confirmed satisfaction and safety compliance.", admin, LocalDateTime.now().minusDays(1))
                    )
            );
        }
    }

    private static class StatusUpdateHistory {
        ComplaintStatus status;
        String message;
        User user;
        LocalDateTime timestamp;

        StatusUpdateHistory(ComplaintStatus status, String message, User user, LocalDateTime timestamp) {
            this.status = status;
            this.message = message;
            this.user = user;
            this.timestamp = timestamp;
        }
    }

    private void createSampleComplaint(
            String code, String title, String description, String category,
            ComplaintPriority priority, ComplaintStatus status,
            double lat, double lng, String address,
            User citizen, Department dept, User officer, int upvotes,
            List<StatusUpdateHistory> history) {

        Complaint c = new Complaint();
        c.setComplaintId(code);
        c.setTitle(title);
        c.setDescription(description);
        c.setCategory(category);
        c.setPriority(priority);
        c.setStatus(status);
        c.setLatitude(lat);
        c.setLongitude(lng);
        c.setAddress(address);
        c.setCitizen(citizen);
        c.setDepartment(dept);
        c.setAssignedOfficer(officer);
        c.setUpvoteCount(upvotes);

        Complaint saved = complaintRepository.save(c);

        for (StatusUpdateHistory h : history) {
            ComplaintUpdate u = new ComplaintUpdate(saved, h.status, h.message, h.user != null ? h.user : citizen);
            u.setCreatedAt(h.timestamp);
            complaintUpdateRepository.save(u);
            saved.addUpdate(u);
        }
    }
}
