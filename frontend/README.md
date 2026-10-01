# Hospital Appointment Management System — Frontend

An Angular 19 single-page application for managing hospital appointments across three roles: Patient, Doctor, and Admin. All API calls are routed through a Spring Cloud Gateway backend.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Framework | Angular 19 (NgModule architecture) |
| Language | TypeScript 5 |
| HTTP | Angular HttpClient with JWT interceptor |
| Auth | JWT stored in localStorage, decoded via `jwt-decode` |
| Styling | CSS custom properties (no external UI library) |
| Build | Angular CLI 21 / Vite |

---

## Prerequisites

- Node.js 20+
- npm 10+
- All backend services running (see [Backend README](../Hospital_Appointment_Management_System_Backend/README.md))

---

## Getting Started

```bash
# Install dependencies
npm install

# Start the development server
ng serve

# Open in browser
http://localhost:4200
```

To point the app at a different gateway, edit [`src/environments/environment.ts`](src/environments/environment.ts):

```typescript
export const environment = {
  production: false,
  gatewayUrl: 'http://localhost:8090'
};
```

---

## Project Structure

```
src/app/
├── core/
│   ├── auth/           auth.service.ts, auth.interceptor.ts, role.guard.ts
│   ├── error/          error.service.ts
│   └── services/       appointment, doctor, patient, medical-history services
├── features/
│   ├── auth/           login, register
│   ├── patient/        dashboard, book-appointment, my-appointments,
│   │                   patient-profile, medical-history
│   ├── doctor/         dashboard, doctor-appointments, doctor-schedule,
│   │                   doctor-profile
│   ├── admin/          dashboard, admin-appointments, admin-doctors,
│   │                   admin-patients
│   └── not-found/
└── shared/
    ├── components/     toast, navbar
    └── models/         TypeScript interfaces
```

---

## Roles and Routes

| Role | Login redirects to | Key routes |
|---|---|---|
| Patient | `/patient/dashboard` | `/patient/appointments`, `/patient/book`, `/patient/profile`, `/patient/medical-history` |
| Doctor | `/doctor/dashboard` | `/doctor/appointments`, `/doctor/schedule`, `/doctor/profile` |
| Admin | `/admin/dashboard` | `/admin/appointments`, `/admin/doctors`, `/admin/patients` |

Route guards (`RoleGuard`) redirect wrong-role users to their own dashboard instead of the login page.

---

## Authentication Flow

1. User logs in — backend returns `{ token, userId, serviceId, role }`.
2. Token is stored in `localStorage` keyed by role (`hams_token_patient`, etc.).
3. Every outgoing HTTP request gets `Authorization: Bearer <token>` added by `AuthInterceptor`.
4. The gateway validates the JWT and forwards `X-User-Role` and `X-Service-Id` headers to each microservice.
5. On logout the token is removed and the user is sent to `/login`.

---

## Key Features by Role

### Patient
- **Book Appointment** — 3-step wizard: choose specialization → select doctor → pick date and time slot.
- **My Appointments** — view upcoming and past appointments; cancel or reschedule from the list.
- **Medical History** — read-only view of all records with doctor name resolution.
- **Profile** — view and update personal details.

### Doctor
- **Appointments** — tabbed upcoming/past view with search and pagination; mark complete, cancel, write or edit prescriptions.
- **Schedule** — create availability for a date (shift start/end, optional break); generates 30-minute slots automatically.
- **Profile** — view professional details.

### Admin
- **Appointments** — full list with status badges and cancel action.
- **Doctors** — list all registered doctors.
- **Patients** — list all registered patients.

---

## Build for Production

```bash
ng build --configuration production
```

Output is in `dist/`. Set `environment.production = true` and confirm `gatewayUrl` points to the production gateway before building.

---

## Running Tests

```bash
ng test
```

Tests use Vitest (configured via Angular CLI).

---

## Environment Variables

| Variable | Default | Purpose |
|---|---|---|
| `gatewayUrl` | `http://localhost:8090` | Base URL of the Spring Cloud Gateway |

---

## Common Issues

**"Network error — cannot connect to the server (port 8090)"**
Ensure the API Gateway and all backend microservices are running before launching the frontend.

**Login succeeds but dashboard is blank**
Check the browser console for JWT decode errors. Confirm the `JWT_SECRET` in the gateway matches the one used by `auth-service`.

**Appointments or doctors not loading**
Open the browser Network tab and confirm the gateway is forwarding requests. Eureka must have all services registered.
