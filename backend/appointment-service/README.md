# Appointment Service - Documentation Index

Welcome to the Appointment Service microservice. This document provides a quick reference to all available documentation.

## 📚 Documentation Files

### 🎯 Quick Start Guides
1. **[LOGGING_QUICK_START.md](LOGGING_QUICK_START.md)** ⭐ START HERE
   - How to run the application with logging
   - API endpoint examples
   - Expected log outputs
   - Real-time log monitoring
   - Troubleshooting guide

2. **[LOGGING_STATUS.md](LOGGING_STATUS.md)**
   - Implementation completion checklist
   - Verification results
   - Code metrics
   - Expected log output examples

### 📖 Comprehensive Guides
3. **[LOGGING_IMPLEMENTATION.md](LOGGING_IMPLEMENTATION.md)**
   - Detailed logging architecture
   - All logged components
   - Configuration files explanation
   - Log levels and patterns
   - Production recommendations
   - Testing instructions

4. **[IMPLEMENTATION_COMPLETE.md](IMPLEMENTATION_COMPLETE.md)**
   - Complete project overview
   - Architecture diagram
   - All features implemented
   - Data model details
   - Repository queries
   - Feign client integration
   - Production checklist

5. **[LOGGING_CHANGELOG.md](LOGGING_CHANGELOG.md)**
   - Detailed list of all file modifications
   - New files created
   - Change-by-change breakdown
   - Testing procedures
   - Deployment instructions

### 📋 Original Documentation
6. **[HELP.md](HELP.md)**
   - Spring Boot reference documentation
   - Building and running the application

---

## 🚀 Quick Start (5 Minutes)

### 1. Run in Development Mode
```bash
mvn clean spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=dev"
```

### 2. Test API Endpoints
```bash
# Get available doctors
curl -X POST http://localhost:8082/api/v1/appointments/available-doctors \
  -H "Content-Type: application/json" \
  -d '{"patientId": 101, "specialization": "Cardiology"}'

# Get available slots
curl -X POST http://localhost:8082/api/v1/appointments/available-slots \
  -H "Content-Type: application/json" \
  -d '{"patientId": 101, "doctorId": 5, "date": "2026-04-10"}'

# Book appointment
curl -X POST http://localhost:8082/api/v1/appointments/booking \
  -H "Content-Type: application/json" \
  -d '{"patientId": 101, "doctorId": 5, "startTime": "2026-04-10T14:00:00"}'
```

### 3. Watch Logs
Console logs will display with timestamps, log levels, and context information.

---

## 🏗️ Architecture Overview

```
Appointment Service (Port 8082)
├── REST API Layer (5 endpoints)
├── Business Logic Layer (AppointmentService)
├── Data Access Layer (JPA Repository)
└── Remote Service Integration
    ├── DoctorSchedule Service (Feign)
    └── Notification Service (Feign)
```

---

## ✨ Key Features

### 1. 3-Step Booking Workflow
1. **Select Specialization** → Get available doctors
2. **Select Doctor** → Get available time slots
3. **Book Appointment** → Confirm booking with confirmation code

### 2. Appointment Management
- ✅ Book appointments with confirmation codes
- ✅ Cancel appointments and release slots
- ✅ Reschedule to different time slots
- ✅ Notifications on status changes

### 3. Security
- UUID-based confirmation codes (not sequential IDs)
- Prevents resource enumeration
- Secure patient/doctor reference

### 4. Concurrency Control
- Optimistic locking with @Version
- Prevents double-booking

### 5. Comprehensive Logging
- Service layer: Business logic tracing
- Controller layer: Request/response tracking
- Exception handler: Error logging
- Feign client: HTTP request/response logging
- Profile-based: dev (DEBUG) vs prod (INFO)

---

## 📊 Project Statistics

| Metric | Count |
|--------|-------|
| REST Endpoints | 5 |
| Service Methods | 5 |
| Feign Clients | 2 |
| Logger Instances | 4 |
| Log Statements | 50+ |
| Documentation Files | 8 |
| Compilation Errors | 0 |
| Test Coverage Ready | ✅ |

---

## 📁 File Structure

```
appointment-service/
├── src/main/java/com/hospital/appointmentservice/
│   ├── client/
│   │   ├── DoctorServiceClient.java          [Feign]
│   │   └── NotificationServiceClient.java    [Feign]
│   ├── config/
│   │   └── FeignLoggingConfig.java           [Feign logging setup]
│   ├── controller/
│   │   ├── AppointmentController.java        [5 REST endpoints]
│   │   └── GlobalExceptionHandler.java       [Exception handling]
│   ├── dto/
│   │   ├── DoctorAvailabilityDTO.java
│   │   ├── TimeSlotDTO.java
│   │   ├── BookAppointmentRequest.java
│   │   ├── RescheduleAppointmentRequest.java
│   │   ├── GetAvailableDoctorsRequest.java
│   │   ├── GetTimeSlotsRequest.java
│   │   └── NotificationRequest.java
│   ├── model/
│   │   ├── Appointment.java                  [JPA Entity]
│   │   └── AppointmentStatus.java
│   ├── repository/
│   │   └── AppointmentRepository.java        [JPA queries]
│   └── service/
│       └── AppointmentService.java           [Business logic]
│
├── src/main/resources/
│   ├── application.properties                [App config + logging levels]
│   └── logback-spring.xml                    [Logback configuration]
│
├── Documentation/
│   ├── LOGGING_QUICK_START.md               [Quick start guide]
│   ├── LOGGING_IMPLEMENTATION.md            [Technical details]
│   ├── IMPLEMENTATION_COMPLETE.md           [Project summary]
│   ├── LOGGING_CHANGELOG.md                 [Change details]
│   ├── LOGGING_STATUS.md                    [Status & verification]
│   ├── README.md                            [This file]
│   └── HELP.md                              [Spring Boot help]
│
└── pom.xml                                   [Maven config]
```

---

## 🔧 Common Commands

### Build
```bash
mvn clean package
```

### Run - Development (Debug Logs)
```bash
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=dev"
```

### Run - Production (Info Logs Only)
```bash
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=prod"
```

### Run JAR
```bash
java -jar target/appointmentservice-1.0.0.jar
```

### View Logs - Development
```bash
# Real-time console output visible when running
# Also check log files in %TEMP%
Get-Content -Path $ENV:TEMP\spring.log -Wait -Tail 100
```

### Check Compilation
```bash
mvn clean compile
```

---

## 🧪 API Testing

### Endpoint 1: Get Available Doctors
```bash
curl -X POST http://localhost:8082/api/v1/appointments/available-doctors \
  -H "Content-Type: application/json" \
  -d '{"patientId": 101, "specialization": "Cardiology"}'
```
**Response**: List of doctors with that specialization

### Endpoint 2: Get Available Slots
```bash
curl -X POST http://localhost:8082/api/v1/appointments/available-slots \
  -H "Content-Type: application/json" \
  -d '{"patientId": 101, "doctorId": 5, "date": "2026-04-10"}'
```
**Response**: List of available time slots for that doctor on that date

### Endpoint 3: Book Appointment
```bash
curl -X POST http://localhost:8082/api/v1/appointments/booking \
  -H "Content-Type: application/json" \
  -d '{"patientId": 101, "doctorId": 5, "startTime": "2026-04-10T14:00:00"}'
```
**Response**: Appointment with confirmationCode (save this!)

### Endpoint 4: Cancel Appointment
```bash
curl -X PUT http://localhost:8082/api/v1/appointments/cancel/{confirmationCode}
```
**Response**: Cancelled appointment

### Endpoint 5: Reschedule Appointment
```bash
curl -X PUT http://localhost:8082/api/v1/appointments/reschedule \
  -H "Content-Type: application/json" \
  -d '{"confirmationCode": "{confirmationCode}", "newAppointmentTime": "2026-04-15T10:00:00"}'
```
**Response**: Rescheduled appointment with new time

---

## 🛠️ Troubleshooting

### Issue: Application won't start
**Solution**: Check MySQL is running on localhost:3306 with credentials (root/root)

### Issue: No logs appearing
**Solution**: Verify spring.profiles.active is set to "dev" for console output

### Issue: Logs too verbose
**Solution**: Change logging.level.root in application.properties to WARN

### Issue: Feature X not working
**Solution**: Check corresponding log files (spring.log, error.log) in %TEMP%

See **[LOGGING_QUICK_START.md](LOGGING_QUICK_START.md)** for more troubleshooting

---

## 📝 Notes

- All APIs require valid patientId and doctorId
- Appointment dates must be in the future
- Confirmation codes are UUIDs (non-sequential for security)
- Notifications sent asynchronously (non-blocking)
- Database: MySQL, schema auto-created via Hibernate

---

## 🎓 Learning Resources

1. **Spring Boot**: See [HELP.md](HELP.md)
2. **Logging**: See [LOGGING_IMPLEMENTATION.md](LOGGING_IMPLEMENTATION.md)
3. **API Usage**: See [LOGGING_QUICK_START.md](LOGGING_QUICK_START.md)
4. **Architecture**: See [IMPLEMENTATION_COMPLETE.md](IMPLEMENTATION_COMPLETE.md)

---

## ✅ Verification

- ✅ Zero compilation errors
- ✅ Zero runtime errors  
- ✅ All endpoints tested
- ✅ Logging fully implemented
- ✅ Production ready
- ✅ Well documented

---

## 📧 Support

For questions or issues, refer to the documentation files or check the detailed logging output.

---

**Last Updated**: January 2026
**Status**: ✅ Production Ready
**Version**: 1.0.0
