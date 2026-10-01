export interface Patient {
  patientId: number;
  name: string;
  dateOfBirth: string;
  gender: 'MALE' | 'FEMALE' | 'OTHER';
  contactDetails: string;
}
