# CivicFix — Civic Issue Reporting & Management Platform

> **"Report. Track. Fix Your City."**

CivicFix is a complete, full-stack municipal civic issue reporting, monitoring, and resolution platform built using **Java, Spring Boot, Spring Security, Spring Data JPA, MySQL/H2**, and a modern responsive **HTML/CSS/JavaScript + Leaflet.js** frontend.

---

## 🚀 Key Features

* **Interactive Issue Reporting**: Report potholes, waterlogging, garbage, streetlights, broken roads, water leaks, open drainage, fallen trees, traffic signal problems, and general civic issues.
* **Leaflet + OpenStreetMap Integration**: Interactive map pin placement, "Use My Location" GPS geolocation, and real-time reverse address lookup.
* **Proactive Duplicate Detection**: Spatial Haversine distance detection warns citizens if a similar issue in the same category was already reported within 100 meters, with quick options to *"View Existing Issue"* or *"Submit Anyway"*.
* **Citizen Upvoting**: Citizens can upvote public grievances to highlight urgency to municipal authorities.
* **7-Stage Visual Status Lifecycle**:
  `Submitted → Under Review → Verified → Assigned → In Progress → Resolved → Closed` *(or `Rejected`)* with dynamic status timeline nodes and audit notes.
* **Role-Based Portals**:
  - **Citizen Portal**: My Reports dashboard, live tracking by `CIV-2026-XXXXX` code, upvoting, in-app notifications.
  - **Field Officer Portal**: Dedicated queue of assigned tasks, progress notes update, photographic resolution proof upload.
  - **Municipal Admin Portal**: Executive KPI metrics, Chart.js analytics (by Category, Status, Priority), complaint verification, department & officer assignment, and status governance.
* **In-App Notification Center**: Unread count badge and notifications whenever a complaint is submitted, verified, assigned, or resolved.
* **Dual Database Engine**:
  - Out-of-the-box **H2 embedded database** for instant, zero-configuration local execution.
  - Full **MySQL 8.x support** with connection pooling and schema DDL scripts.

---

## 🛠 Tech Stack

* **Backend**: Java 17, Spring Boot 3.2.3 (Spring Web, Spring Security, Spring Data JPA, Spring Validation)
* **Frontend**: HTML5, Tailwind CSS, Vanilla JavaScript (ES6+), Leaflet.js, Chart.js, FontAwesome 6
* **Database**: MySQL 8.x / H2 Embedded Engine
* **Authentication**: Stateless JWT (JSON Web Tokens) with BCrypt password hashing
* **Build Tool**: Apache Maven

---

## 📋 Pre-configured Demo Accounts

CivicFix comes pre-seeded with realistic demonstration data, departments, and credentials:

| Role | Email | Password | Description |
| :--- | :--- | :--- | :--- |
| **Municipal Admin** | `admin@civicfix.gov` | `Admin@123` | Full administrative control, chart analytics, assignments |
| **Road Dept Officer** | `officer.road@civicfix.gov` | `Officer@123` | Assigned to road and infrastructure tasks |
| **Water Dept Officer** | `officer.water@civicfix.gov` | `Officer@123` | Assigned to water supply & leaks |
| **Citizen** | `citizen@example.com` | `Citizen@123` | Standard citizen account with sample complaints |
| **Citizen (Priya)** | `priya.sharma@example.com` | `Citizen@123` | Active citizen with upvoted complaints |

*Tip: The top banner of the website includes one-click demo login buttons for rapid testing.*

---

## 💻 Step-by-Step Setup Guide

### 1. Prerequisites: Java & Maven
CivicFix requires **Java 17 or higher** and **Apache Maven 3.8+**.
- Verify Java:
  ```powershell
  java -version
  ```
- Verify Maven:
  ```powershell
  mvn -version
  ```

---

### 2. Database Options

#### Option A: Instant Run with H2 (Default — Zero Setup)
The application is preconfigured with `spring.profiles.active=dev` in `src/main/resources/application.properties`. It will store data in `./data/civicfixdb` and initialize all tables and demo records automatically without needing a MySQL server running!

#### Option B: Connecting to MySQL 8.x
1. Start your local MySQL service or run the included Docker Compose container:
   ```powershell
   docker-compose up -d
   ```
2. Create the database in MySQL:
   ```sql
   CREATE DATABASE IF NOT EXISTS civicfix_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   ```
3. Update `src/main/resources/application.properties`:
   ```properties
   spring.profiles.active=mysql
   ```
   Or set your MySQL username and password in `src/main/resources/application-mysql.properties`:
   ```properties
   spring.datasource.username=root
   spring.datasource.password=your_mysql_password
   ```

---

### 3. Building the Project
Navigate to the project root directory and build the JAR:
```powershell
cd C:\Users\Jeet\.gemini\antigravity\scratch\civicfix
mvn clean package -DskipTests
```

---

### 4. Running the Application

You can start the application using any of the following methods:

**Method 1: Maven Spring Boot Plugin**
```powershell
mvn spring-boot:run
```

**Method 2: One-Click Windows Script**
Double-click `run.bat` or in PowerShell execute:
```powershell
.\run.ps1
```

**Method 3: Executing the Packaged JAR**
```powershell
java -jar target/civicfix-1.0.0.jar
```

---

### 5. Accessing the Platform

Open your web browser and navigate to:
```
http://localhost:8080
```

* **Main Application UI**: `http://localhost:8080/`
* **H2 Database Console** (when running in dev mode): `http://localhost:8080/h2-console`
  - JDBC URL: `jdbc:h2:file:./data/civicfixdb`
  - User: `sa`
  - Password: *(leave blank)*

---

## 🧪 Testing & Demonstration Walkthrough

### 1. Citizen Registration & Login
1. Click **Register** in the navigation bar.
2. Fill in: Name, Email, Mobile Number, and Password.
3. Click **Create Account**. You will receive an immediate welcome alert and access to the Citizen Dashboard.

### 2. Reporting a Civic Grievance
1. Click **Report Issue** in the navigation bar.
2. Select a category (e.g., *Potholes*).
3. Enter title: *"Deep crater near street corner"*.
4. Select priority: *High* or *Critical*.
5. Click on the Leaflet map to pinpoint the exact location, or click **Use My Location (GPS)**.
6. Attach a photo (JPG/PNG).
7. Notice the real-time **Duplicate Detection** feature: If coordinates match within 100m of an existing pothole, an alert banner will recommend checking the existing report or continuing.
8. Click **Submit Complaint**.
9. A confirmation modal will display your new unique tracking code (e.g., `CIV-2026-00009`).

### 3. Tracking Complaint Lifecycle
1. Click **Track My Issue** in the navigation or enter any code into the search box.
2. Inspect the **7-stage visual horizontal progress timeline**:
   - Nodes turn green with checks as milestones are achieved.
   - Active milestone displays an animated blue pulse.
3. Review the location map, attached evidence photos, and official audit log entries.
4. Click the **Upvote** button to increment community urgency.

### 4. Municipal Admin Governance
1. Click the quick-login **Admin** button or log in as `admin@civicfix.gov` / `Admin@123`.
2. Go to **Admin Analytics & Staff**:
   - View real-time KPIs and 3 Chart.js graphs (Category doughnut, Status bar, Priority pie).
   - Click **Verify** on any new complaint to validate it.
   - Click **Assign** to select a department (e.g. *Road Department*) and assign an officer.
   - Click **Status** to reject (with reason) or transition status.

### 5. Field Officer Execution
1. Log in as `officer.road@civicfix.gov` / `Officer@123`.
2. Go to **Officer Work Queue**:
   - View only complaints assigned to your department/account.
   - Click **Update Work** on an assigned issue.
   - Change status to **In Progress** or **Resolved**, provide repair notes, and upload a completion photo.
   - Click **Submit Update**.
3. Log in back as the reporting Citizen and observe the new **In-App Notification** in the top navigation bell indicating your issue has been fixed!

---

## 📡 REST API Reference

| Method | Endpoint | Access | Description |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/auth/register` | Public | Register a new citizen account |
| `POST` | `/api/auth/login` | Public | Authenticate user and receive JWT token |
| `GET` | `/api/auth/me` | Authenticated | Retrieve current user profile |
| `POST` | `/api/complaints` | Authenticated | Submit complaint with multipart file evidence |
| `POST` | `/api/complaints/check-duplicate` | Public | Check for nearby complaints within radius |
| `GET` | `/api/complaints/public` | Public | Explorer list with category/status/keyword filters |
| `GET` | `/api/complaints/by-code/{code}` | Public | Public tracking lookup by `CIV-2026-XXXXX` |
| `GET` | `/api/complaints/my` | Authenticated | Citizen's reported complaints list |
| `POST` | `/api/complaints/{id}/upvote` | Authenticated | Toggle upvote on a complaint |
| `GET` | `/api/statistics/summary` | Public | Aggregate counts for landing hero counters |
| `GET` | `/api/admin/statistics` | Admin | Comprehensive chart and KPI statistics |
| `GET` | `/api/admin/complaints` | Admin | Administrative complaints governance queue |
| `PUT` | `/api/admin/complaints/{id}/assign` | Admin | Assign department and field officer |
| `PUT` | `/api/admin/complaints/{id}/status` | Admin | Update status / reject with reason |
| `GET` | `/api/officer/complaints` | Officer/Admin | View assigned complaints queue |
| `POST` | `/api/officer/complaints/{id}/updates` | Officer/Admin | Post field update with proof photo |
| `GET` | `/api/notifications` | Authenticated | Fetch user in-app notification alerts |
| `PUT` | `/api/notifications/{id}/read` | Authenticated | Mark individual notification as read |
| `PUT` | `/api/notifications/read-all` | Authenticated | Mark all notifications as read |
