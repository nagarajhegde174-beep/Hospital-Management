import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { SharedModule } from '../../shared/shared-module';
import { PatientDashboard } from './dashboard/patient-dashboard';
import { BookAppointment } from './book-appointment/book-appointment';
import { MyAppointments } from './my-appointments/my-appointments';
import { PatientProfile } from './patient-profile/patient-profile';
import { MedicalHistoryPage } from './medical-history/medical-history';

const routes: Routes = [
  { path: 'dashboard',        component: PatientDashboard },
  { path: 'book-appointment', component: BookAppointment },
  { path: 'appointments',     component: MyAppointments },
  { path: 'profile',          component: PatientProfile },
  { path: 'medical-history',  component: MedicalHistoryPage },
  { path: '',                 redirectTo: 'dashboard', pathMatch: 'full' }
];

@NgModule({
  declarations: [
    PatientDashboard,
    BookAppointment,
    MyAppointments,
    PatientProfile,
    MedicalHistoryPage
  ],
  imports: [SharedModule, RouterModule.forChild(routes)]
})
export class PatientModule {}
