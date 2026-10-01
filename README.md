# Hospital Appointment Management System (HAMS)

A full-stack hospital appointment management platform built with a Spring Boot microservices backend and an Angular frontend.

## Repository Layout

- `Hospital_Appointment_Management_System_Backend/` – Spring Boot microservices backend with Eureka service discovery and API Gateway.
- `hospital-appointment-management-system-frontend/` – Angular SPA frontend.

## Overview

HAMS provides appointment management for three roles:

- **Patient**: register, login, book, reschedule, and cancel appointments, view medical history, and manage a patient profile.
- **Doctor**: register, login, publish availability, manage appointments, complete appointments, add prescriptions, and update a doctor profile.
- **Admin**: view and manage all appointments, doctors, and patients.

The frontend is built in **Angular**, while the backend is implemented as a set of Spring Boot microservices.

## Architecture

The backend is a microservice ecosystem coordinated by Eureka service discovery and an API Gateway.

### Core flow

1. Frontend sends requests to the API Gateway at `http://localhost:8090`.
2. API Gateway validates JWT tokens and appends security headers.
3. Gateway routes requests to the appropriate microservice.
4. Downstream services perform authorization checks, execute business logic, and respond.
5. Appointment-related events can trigger the `notification-service` to send emails.

## Services

| Service | Port | Responsibility |
|---|---|---|
| `eureka-server` | 8761 | Service registry and discovery |
| `api-gateway` | 8090 | JWT validation, routing, and header injection |
| `auth-service` | 8091 | User registration, login, JWT issuance, and identity lookup |
| `patient-service` | 8081 | Patient profile CRUD |
| `doctor-profile-service` | 8082 | Doctor profile CRUD and specialization search |
| `appointment-service` | 8083 | Appointment booking, cancellation, rescheduling, completion |
| `doctor-service` | 8084 | Doctor availability, slot generation, prescriptions |
| `medical-history-service` | 8085 | Medical history CRUD |
| `notification-service` | 8086 | Email notifications for appointment events |

## How the Backend Works

### Authentication and Authorization

- `auth-service` issues JWTs and returns user role, user ID, and service ID.
- The gateway validates the JWT on every request.
- It forwards the headers `X-User-Role` and `X-Service-Id` to downstream services.
- Downstream services use those values for ownership and role checks.
- Valid roles include `ROLE_PATIENT`, `ROLE_DOCTOR`, and `ROLE_ADMIN`.

### Database Ownership

Each service owns its own MySQL schema, preventing schema coupling and enforcing service autonomy:

- `auth_db`
- `patient_db`
- `doctor_db`
- `appointment_db`
- `medical_history_db`

### Appointment Lifecycle

1. Patient chooses a specialization and doctor in the frontend.
2. Frontend requests doctor details from `doctor-profile-service`.
3. Frontend fetches doctor availability and slots from `doctor-service`.
4. Patient selects a slot and books via `appointment-service`.
5. `appointment-service` stores the appointment and returns confirmation.
6. If appointment status changes, `appointment-service` calls `notification-service`.
7. `notification-service` sends emails to both patient and doctor.
8. Doctor can complete, cancel, or reschedule appointments; `appointment-service` handles the logic.

### Slot Generation

- Doctors publish availability in `doctor-service`.
- The service auto-generates 30-minute slots from `shiftStart` to `shiftEnd`.
- A short break is inserted after every three slots.
- If configured, a long break removes overlapping slots.
- Slots are not created if they would cross a break boundary.

### Notification Flow

- Appointment events trigger calls from `appointment-service` to `notification-service`.
- `notification-service` fetches email addresses from `auth-service`.
- Email delivery uses Gmail SMTP if `MAIL_PASSWORD` is configured.
- If mail credentials are absent, the booking still succeeds; only notifications are skipped.

## How the Frontend Works

### Angular Frontend

The frontend is implemented with Angular and TypeScript. It communicates with the backend through the API Gateway and uses a token-based auth flow.

### Frontend Request Flow

1. User logs in or registers through the Angular auth module.
2. The backend returns a JWT.
3. The app stores the token in `localStorage`.
4. `AuthInterceptor` attaches `Authorization: Bearer <token>` to HTTP requests.
5. Route guards protect pages based on role.

### User Journeys

#### Patient

- Browse doctors by specialization.
- Select a doctor and desired date.
- View available appointment slots.
- Book, reschedule, or cancel appointments.
- View personal profile and medical history.

#### Doctor

- Create availability schedules.
- View and manage booked appointments.
- Mark appointments complete and add prescriptions.

#### Admin

- Review all appointments.
- View lists of doctors and patients.

### Frontend Modules

- `core/auth/` – login, auth service, token interceptor, role guard.
- `core/services/` – backend API service clients.
- `features/auth/` – login and registration views.
- `features/patient/` – patient dashboard, booking, appointments, profile, medical history.
- `features/doctor/` – doctor dashboard, schedule creation, appointments, profile.
- `features/admin/` – admin dashboards and management pages.
- `shared/components/` – reusable UI elements such as navbar and toast notifications.

### Booking Flow

1. Patient selects a specialization.
2. The app fetches doctors from `doctor-profile-service`.
3. Patient chooses a doctor and date.
4. Available time slots are fetched from `doctor-service`.
5. The appointment is confirmed with `appointment-service`.

## Setup

1. Clone the repository.
2. Start MySQL and create the required databases.

```sql
CREATE DATABASE auth_db;
CREATE DATABASE patient_db;
CREATE DATABASE doctor_db;
CREATE DATABASE appointment_db;
CREATE DATABASE medical_history_db;
```

3. Configure environment variables for backend and frontend.

## Environment Variables

### Backend

- `JWT_SECRET` – JWT signing key used by `api-gateway` and `auth-service`.
- `DB_PASSWORD` – MySQL password used by all data services.
- `MAIL_PASSWORD` – Gmail App Password used by `notification-service`.

### Frontend

- `hospital-appointment-management-system-frontend/src/environments/environment.ts` – set `gatewayUrl` to the API Gateway URL.

## Running the Backend

Start Eureka first:

```powershell
cd .\Hospital_Appointment_Management_System_Backend\eureka-server
./mvnw spring-boot:run
```

Then start service nodes:

```powershell
cd ..\auth-service
./mvnw spring-boot:run

cd ..\patient-service
./mvnw spring-boot:run

cd ..\doctor-profile-service
./mvnw spring-boot:run

cd ..\doctor-service
./mvnw spring-boot:run

cd ..\medical-history-service
./mvnw spring-boot:run

cd ..\appointment-service
./mvnw spring-boot:run

cd ..\notification-service
./mvnw spring-boot:run
```

Finally start the gateway:

```powershell
cd ..\api-gateway
./mvnw spring-boot:run
```

Confirm service registration at `http://localhost:8761`.

## Running the Frontend

```powershell
cd .\hospital-appointment-management-system-frontend
npm install
npm start
```

Open the application at `http://localhost:4200`.

## Build Commands

- Backend: `mvn clean package -DskipTests` from `Hospital_Appointment_Management_System_Backend/`.
- Frontend: `ng build --configuration production` from `hospital-appointment-management-system-frontend/`.

## Testing

- Backend tests: run `./mvnw test` inside each backend service folder.
- Frontend tests: run `npm test` inside `hospital-appointment-management-system-frontend/`.

## Common Issues

- `401 Unauthorized` from the gateway: ensure `JWT_SECRET` matches between `api-gateway` and `auth-service`.
- Missing services in Eureka: check that all services use `eureka.client.service-url.defaultZone=http://localhost:8761/eureka/`.
- Frontend cannot connect: verify `gatewayUrl` in `environment.ts` and that the gateway is running.
- Emails not sending: configure `MAIL_PASSWORD` for Gmail SMTP, or leave blank to skip mail delivery.

## Documentation

- Backend README: `Hospital_Appointment_Management_System_Backend/README.md`
- Frontend README: `hospital-appointment-management-system-frontend/README.md`
