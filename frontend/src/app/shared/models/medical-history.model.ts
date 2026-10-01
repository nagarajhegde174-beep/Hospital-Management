export interface MedicalHistory {
  recordId: number;
  patientId: number;
  doctorId?: number;
  diagnosis: string;
  diagnosedAt: string;
  prescribedMeds: string[];
}
