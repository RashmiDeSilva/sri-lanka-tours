# Sri Lanka Tour Guide System

A full-stack web application that connects **tourists** with **certified local tour guides** across Sri Lanka. Tourists can explore destinations, book guides, plan custom itineraries, and leave reviews. Guides can manage their profiles and respond to booking requests. Admins have full system control.

---

## 🛠 Technology Stack

| Layer | Technology |
|---|---|
| **Backend** | Java 17, Spring Boot 3, Spring MVC, Spring Security |
| **Data Base** | MySQL 8 |
| **Frontend** | Thymeleaf, HTML5, CSS3, JavaScript |
| **Styling** | Tailwind CSS (CDN), Lucide Icons |
| **Build Tool** | Apache Maven |
| **Security** | Spring Security (BCrypt, Form Login) |

---

## 📐 Architecture

```
src/main/java/com/example/tour_guide_system/
├── controller/        # Spring MVC Controllers (HTTP layer)
├── entity/            # JPA Entities (Domain Model)
├── repository/        # Spring Data JPA Repositories (Data Access)
├── service/           # Business Logic Layer
├── security/          # Spring Security Configuration
└── exception/         # Custom Exception Handlers
```

---

## 🔑 Key Features & Functions

### 1. 👤 User Management (CRUD)
Manages **Tourists**, **Tour Guides**, and **Admins** with role-based access.

| Operation | Who | Endpoint / Class |
|---|---|---|
| **Create** | Self-registration | `POST /register` → `AuthController` |
| **Read** | Admin views all users | `GET /admin/users` → `AdminPanelController` |
| **Update** | Tourist updates profile | `POST /tourist/profile/update` → `TouristController` |
| **Update** | Guide updates profile | `POST /home/tourguide/{id}/profile/update` → `TourGuideController` |
| **Delete** | Admin blocks / deletes | `POST /admin/users/{id}/block` → `AdminPanelController` |

**Entity:** [`User.java`](src/main/java/com/example/tour_guide_system/entity/User.java)  
**Repository:** [`UserRepository.java`](src/main/java/com/example/tour_guide_system/repository/UserRepository.java)  
**Service:** [`UserService.java`](src/main/java/com/example/tour_guide_system/service/UserService.java)

---

### 2. 🗺 Destination Management (CRUD)
Admins manage all tourist destinations in Sri Lanka with photos, province, entry fees, and ratings.

| Operation | Who | Endpoint / Class |
|---|---|---|
| **Create** | Admin adds destination | `POST /admin/destinations/create` → `AdminPanelController` |
| **Read** | All users browse | `GET /tourist/destinations` → `TouristController` |
| **Update** | Admin edits destination | `POST /admin/destinations/{id}/edit` → `AdminPanelController` |
| **Delete** | Admin removes destination | `POST /admin/destinations/{id}/delete` → `AdminPanelController` |

**Entity:** [`Destination.java`](src/main/java/com/example/tour_guide_system/entity/Destination.java)  
**Repository:** [`DestinationRepository.java`](src/main/java/com/example/tour_guide_system/repository/DestinationRepository.java)  
**Service:** [`DestinationService.java`](src/main/java/com/example/tour_guide_system/service/DestinationService.java)

---

### 3. 📅 Tour Plan Management (CRUD)
Tourists build custom multi-destination itineraries, assign a guide, set travel dates, and specify group composition.

| Operation | Who | Endpoint / Class |
|---|---|---|
| **Create** | Tourist submits plan | `POST /tourist/tour-plans/create` → `TouristController` |
| **Read** | Tourist views own plans | `GET /tourist/tour-plans` → `TouristController` |
| **Update** | Tourist edits plan | `POST /tourist/tour-plans/create` (with `bookingId`) → `TouristController` |
| **Delete (Cancel)** | Tourist cancels plan | `POST /tourist/tour-plans/{id}/delete` → `TouristController` |
| **Read** | Guide views assigned plans | `GET /home/tourguide/{id}/tours` → `GuideBookingController` |
| **Update (Accept/Reject)** | Guide responds | `POST /home/tourguide/{id}/quotation` → `GuideBookingController` |
| **Read / Update** | Admin manages all plans | `GET /admin/tour-plans` → `AdminPanelController` |

**Entity:** [`TourPlan.java`](src/main/java/com/example/tour_guide_system/entity/TourPlan.java)  
**Repository:** [`TourPlanRepository.java`](src/main/java/com/example/tour_guide_system/repository/TourPlanRepository.java)  
**Service:** [`TourPlanService.java`](src/main/java/com/example/tour_guide_system/service/TourPlanService.java)

---

### 4. 📋 Guide Booking Management (CRUD)
Tourists can directly book a specific guide (without destinations) as a quick booking request.

| Operation | Who | Endpoint / Class |
|---|---|---|
| **Create** | Tourist books a guide | `POST /tourist/guides/{id}/book` → `TouristController` |
| **Read** | Tourist views bookings | `GET /tourist/bookings` → `TouristController` |
| **Update (Accept/Reject)** | Guide responds | `POST /home/tourguide/{id}/quotation` → `GuideBookingController` |
| **Delete (Cancel)** | Tourist cancels booking | `POST /tourist/bookings/{id}/delete` → `TouristController` |

**Service:** [`GuideBookingService.java`](src/main/java/com/example/tour_guide_system/service/GuideBookingService.java)

> A "direct booking" is a `TourPlan` record with no destinations — it represents a tourist's intent to book a guide first, then plan the tour.

---

### 5. ⭐ Review Management (CRUD)
Users can submit reviews on destinations and write general experience reviews about the platform.

| Operation | Who | Endpoint / Class |
|---|---|---|
| **Create** | Tourist submits review | `POST /tourist/destinations/{id}/reviews` → `TouristController` |
| **Create** | Any user adds platform review | `POST /reviews/add` → `ReviewController` |
| **Read** | All users view reviews | `GET /reviews` → `ReviewController` |
| **Update** | Author or Admin edits | `POST /reviews/{id}/edit` → `ReviewController` |
| **Delete** | Author or Admin deletes | `GET /reviews/{id}/delete` → `ReviewController` |

**Entity:** [`Review.java`](src/main/java/com/example/tour_guide_system/entity/Review.java)  
**Repository:** [`ReviewRepository.java`](src/main/java/com/example/tour_guide_system/repository/ReviewRepository.java)  
**Service:** [`ReviewService.java`](src/main/java/com/example/tour_guide_system/service/ReviewService.java)

---

## 👥 User Roles

| Role | Description |
|---|---|
| **Tourist** | Browses destinations & guides, books guides, creates custom tour plans, writes reviews |
| **Guide** | Views assigned bookings/plans, accepts or rejects requests, manages own profile |
| **Admin** | Full CRUD on users, destinations, tour plans, and reviews via the admin panel |

---

## 🗄 Database

- **Database:** MySQL 8  
- **Schema auto-managed by:** Hibernate (`ddl-auto=update`)  
- **Database name:** `t_db` (auto-created if not present)

### Core Tables

| Table | Entity | Description |
|---|---|---|
| `users` | `User` | All users (tourist, guide, admin) |
| `tour_plans` | `TourPlan` | Both direct bookings and custom tour plans |
| `tour_plan_destinations` | Join table | Many-to-many: TourPlan ↔ Destination |
| `destinations` | `Destination` | Sri Lankan tourist destinations |
| `reviews` | `Review` | Destination and platform reviews |

---

## 🚀 Getting Started

### Prerequisites
- Java 17+
- MySQL 8+
- Maven 3.8+

### 1. Clone the Repository
```bash
git clone https://github.com/your-username/tour-guide-system.git
cd tour-guide-system
```

### 2. Configure the Database
Create the database (the app can do this automatically):
```sql
CREATE DATABASE IF NOT EXISTS t_db;
```

Update credentials in `src/main/resources/application.properties`, or set environment variables:
```bash
export SPRING_DATASOURCE_USERNAME=root
export SPRING_DATASOURCE_PASSWORD=your_password
```

### 3. Build & Run
```bash
mvn spring-boot:run
```
The application will start at **http://localhost:8085**

### 4. First-Time Setup
Register an account. To create an Admin, manually update the role in the database:
```sql
UPDATE users SET role = 'ADMIN', approved = 1 WHERE email = 'admin@example.com';
```

---

## 📁 Project Structure

```
tour_guide_system/
├── src/
│   ├── main/
│   │   ├── java/com/example/tour_guide_system/
│   │   │   ├── controller/          # HTTP request handlers
│   │   │   │   ├── AdminController.java
│   │   │   │   ├── AdminPanelController.java
│   │   │   │   ├── AuthController.java
│   │   │   │   ├── GuideBookingController.java
│   │   │   │   ├── HomeController.java
│   │   │   │   ├── ReviewController.java
│   │   │   │   ├── TourGuideController.java
│   │   │   │   ├── TouristController.java
│   │   │   │   └── TouristHomeController.java
│   │   │   ├── entity/              # JPA domain models
│   │   │   │   ├── Destination.java
│   │   │   │   ├── Review.java
│   │   │   │   ├── TourPlan.java
│   │   │   │   └── User.java
│   │   │   ├── repository/          # Data access interfaces
│   │   │   │   ├── DestinationRepository.java
│   │   │   │   ├── ReviewRepository.java
│   │   │   │   ├── TourPlanRepository.java
│   │   │   │   └── UserRepository.java
│   │   │   ├── service/             # Business logic
│   │   │   │   ├── DestinationService.java
│   │   │   │   ├── GuideBookingService.java
│   │   │   │   ├── ReviewService.java
│   │   │   │   ├── TourPlanService.java
│   │   │   │   └── UserService.java
│   │   │   └── security/            # Auth & access control
│   │   │       ├── CustomUserDetails.java
│   │   │       ├── CustomUserDetailsService.java
│   │   │       └── SecurityConfig.java
│   │   └── resources/
│   │       ├── templates/           # Thymeleaf HTML views
│   │       │   ├── fragments/       # Shared header, footer, sidebar
│   │       │   ├── admin/           # Admin panel views
│   │       │   ├── guide/           # Tour guide views
│   │       │   └── tourist/         # Tourist views
│   │       ├── static/
│   │       │   ├── images/          # Static image assets
│   │       │   └── js/              # Client-side JavaScript
│   │       ├── application.properties
│   │       └── schema.sql
│   └── test/
├── pom.xml
└── README.md
```

---

## 🔒 Security

- Passwords hashed with **BCrypt**
- Session-based authentication via **Spring Security**
- Role-based route protection (`TOURIST`, `GUIDE`, `ADMIN`)
- CSRF protection enabled on all state-changing forms

---

## 📄 License

This project was developed as an academic coursework project for the **Software Engineering** module offered in **Year 2, Semester 1** at the **Sri Lanka Institute of Information Technology (SLIIT)**.

