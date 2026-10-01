# Hospital Appointment Management System — Backend

A Spring Boot microservices backend for a hospital appointment management platform. Services communicate via Spring Cloud Netflix Eureka and are exposed to clients through a single API Gateway.

---

## Architecture

```
Angular Frontend (4200)
        |
        v
API Gateway (8090)  <-- validates JWT, injects X-User-Role / X-Service-Id headers
        |
   Eureka (8761) -- service registry
        |
   +-----------+----------+------------------+---------------+------------------+
   |           |          |                  |               |                  |
auth-service  patient-  doctor-profile-   appointment-   doctor-service    medical-history-
  (8091)      service    service (8082)    service (8083)   (8084)           service (8085)
              (8081)                           |
                                    notification-service (8086)
```

All client traffic enters via the gateway. Internal service-to-service calls use Feign clients via Eureka — services never call each other by hard-coded port.

---

## Services

| Service | Port | Responsibility |
|---|---|---|
| `eureka-server` | 8761 | Service registry |
| `api-gateway` | 8090 | JWT validation, routing, header injection |
| `auth-service` | 8091 | Register, login, JWT issuance, email lookup |
| `patient-service` | 8081 | Patient profile CRUD |
| `doctor-profile-service` | 8082 | Doctor profile CRUD |
| `doctor-service` | 8084 | Doctor availability, slot generation, prescriptions |
| `appointment-service` | 8083 | Book, cancel, reschedule, complete appointments |
| `medical-history-service` | 8085 | Medical records CRUD |
| `notification-service` | 8086 | Email notifications on appointment status change |

---

## Prerequisites

- Java 21
- Maven 3.9+
- MySQL 8 running on `localhost:3306`
- (Optional) Gmail App Password for email notifications

---

## Database Setup

Each service that owns data has its own schema. Create them before starting the services:

```sql
CREATE DATABASE auth_db;
CREATE DATABASE patient_db;
CREATE DATABASE doctor_db;
CREATE DATABASE appointment_db;
CREATE DATABASE medical_history_db;
```

Hibernate auto-creates tables on first start (`spring.jpa.hibernate.ddl-auto=update`).

---

## Environment Variables

| Variable | Used by | Purpose |
|---|---|---|
| `JWT_SECRET` | `api-gateway`, `auth-service` | HS256 signing key (min 32 chars). **Required in production.** |
| `DB_PASSWORD` | all data services | MySQL password (default `root` for local dev) |
| `MAIL_PASSWORD` | `notification-service` | Gmail App Password. If blank, email sending is skipped gracefully. |

A fallback development secret is hard-coded for local use only — always override `JWT_SECRET` in production.

---

## Starting the Services

Start in this order (each service must reach Eureka before the next):

```bash
# 1. Service registry
cd eureka-server && mvn spring-boot:run

# 2. Auth and profile services (can start in parallel after Eureka is up)
cd auth-service          && mvn spring-boot:run
cd patient-service       && mvn spring-boot:run
cd doctor-profile-service && mvn spring-boot:run
cd doctor-service        && mvn spring-boot:run
cd medical-history-service && mvn spring-boot:run
cd appointment-service   && mvn spring-boot:run
cd notification-service  && mvn spring-boot:run

# 3. Gateway last (routes need the other services registered)
cd api-gateway && mvn spring-boot:run
```

Verify all services are registered: `http://localhost:8761`

---

## API Overview

All routes are relative to the gateway base `http://localhost:8090`.

### Auth (`/auth-service/api/v1/auth`)

| Method | Path | Description | Auth |
|---|---|---|---|
| POST | `/register` | Register patient or doctor | None |
| POST | `/login` | Login, returns JWT | None |
| GET | `/users?role=PATIENT` | List users by role | Admin |

### Patient (`/patient-service/api/v1/patients`)

| Method | Path | Description | Auth |
|---|---|---|---|
| GET | `/{id}` | Get patient profile | Patient (own) |
| PUT | `/{id}` | Update patient profile | Patient (own) |

### Doctor Profile (`/doctor-profile-service/api/v1/doctors`)

| Method | Path | Description | Auth |
|---|---|---|---|
| GET | `/` | List all doctors | Any |
| GET | `/{id}` | Get doctor by ID | Any |
| GET | `/specialization/{spec}` | Doctors by specialization | Any |
| PUT | `/{id}` | Update doctor profile | Doctor (own) |

### Appointments (`/appointment-service/api/v1/appointments`)

| Method | Path | Description | Auth |
|---|---|---|---|
| GET | `?patientId={id}` | Patient's appointments | Patient (own) |
| GET | `?doctorId={id}` | Doctor's appointments | Doctor (own) |
| GET | `/all` | All appointments | Admin |
| GET | `/available-dates/{doctorId}` | Available booking dates | Any |
| POST | `/available-slots` | Time slots for a date | Any |
| POST | `/booking` | Book an appointment | Patient |
| PATCH | `/cancel/{code}` | Cancel appointment | Patient / Doctor |
| PATCH | `/reschedule` | Reschedule appointment | Patient |
| PATCH | `/complete/{code}` | Mark appointment complete | Doctor |
| GET | `/{code}/patient-history` | Patient's medical history | Doctor |

### Doctor Schedule (`/doctor-service/api/v1/schedule`)

| Method | Path | Description | Auth |
|---|---|---|---|
| POST | `/` | Create availability + slots | Doctor |
| GET | `/slots/{doctorId}?date=` | Get slots for a date | Any |
| POST | `/prescription/{slotId}` | Add prescription via slot | Doctor |

### Medical History (`/medical-history-service/api/v1/records`)

| Method | Path | Description | Auth |
|---|---|---|---|
| GET | `/patient/{id}` | Get patient's records | Patient (own) / Doctor |
| POST | `/` | Add a record | Doctor |
| PUT | `/{id}` | Update a record | Doctor |
| DELETE | `/{id}` | Delete a record | Admin |

---

## Security Model

- The gateway extracts and validates the JWT on every request.
- It forwards two headers to all downstream services:
  - `X-User-Role`: e.g., `ROLE_PATIENT`, `ROLE_DOCTOR`, `ROLE_ADMIN`
  - `X-Service-Id`: the `serviceId` claim from the token (patientId or doctorId)
- Each service uses these headers for ownership checks — a patient cannot access another patient's data.
- Roles stored in the auth DB as `PATIENT` / `DOCTOR` / `ADMIN`; JWT carries them prefixed as `ROLE_PATIENT` etc. (added by Spring Security's `SimpleGrantedAuthority`).

---

## Slot Generation Logic

When a doctor creates availability (`POST /schedule`), `doctor-service` auto-generates 30-minute slots:

- Slots run from `shiftStart` to `shiftEnd`.
- After every 3 consecutive slots a 30-minute short break is inserted.
- If a break window (`breakStart`) is configured, a 60-minute long break is applied at that time.
- Slots that would straddle the break boundary are skipped.
- Availability must be created at least 3 days in advance.

---

## Notification Flow

When an appointment is booked, cancelled, rescheduled, or completed, `appointment-service` calls `notification-service` via Feign. The notification service fetches patient and doctor emails from `auth-service` and sends tailored emails to both parties. If `MAIL_PASSWORD` is not set the service logs a warning and skips sending — the appointment operation itself is unaffected.

---

## Building All Services

From the root (parent `pom.xml`):

```bash
mvn clean package -DskipTests
```

Run individual JARs:

```bash
java -DJWT_SECRET=<secret> -jar auth-service/target/auth-service-*.jar
```

---

## Common Issues

**Services not appearing in Eureka**
Confirm `eureka.client.service-url.defaultZone=http://localhost:8761/eureka/` in each service's `application.properties` and that Eureka started first.

**401 Unauthorized from gateway**
The `JWT_SECRET` in `api-gateway` and `auth-service` must match. Both default to the same dev fallback, so this only occurs if one has been overridden.

**Email not sending**
Set the `MAIL_PASSWORD` environment variable to a Gmail App Password. The sending account must have 2-step verification enabled. Leaving the variable blank disables emails without affecting other functionality.

**Double-booking rejected**
Slots use `@Version` optimistic locking. Concurrent booking attempts for the same slot will result in one succeeding and the other receiving a 409 Conflict.
