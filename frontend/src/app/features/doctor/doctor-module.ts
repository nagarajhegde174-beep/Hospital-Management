import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { SharedModule } from '../../shared/shared-module';
import { DoctorDashboard } from './dashboard/doctor-dashboard';
import { DoctorAppointments } from './doctor-appointments/doctor-appointments';
import { DoctorSchedule } from './doctor-schedule/doctor-schedule';
import { DoctorProfile } from './doctor-profile/doctor-profile';

const routes: Routes = [
  { path: 'dashboard',    component: DoctorDashboard },
  { path: 'appointments', component: DoctorAppointments },
  { path: 'schedule',     component: DoctorSchedule },
  { path: 'profile',      component: DoctorProfile },
  { path: '',             redirectTo: 'dashboard', pathMatch: 'full' }
];

@NgModule({
  declarations: [
    DoctorDashboard,
    DoctorAppointments,
    DoctorSchedule,
    DoctorProfile
  ],
  imports: [SharedModule, RouterModule.forChild(routes)]
})
export class DoctorModule {}
