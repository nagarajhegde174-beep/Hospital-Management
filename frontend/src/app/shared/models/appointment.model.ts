export type AppointmentStatus = 'CONFIRMED' | 'CANCELLED' | 'COMPLETED' | 'RESCHEDULED';

export interface Appointment {
  appointmentId: number;
  patientId: number;
  doctorId: number;
  appointmentDate: string;
  status: AppointmentStatus;
  confirmationCode: string;
  version: number;
}
