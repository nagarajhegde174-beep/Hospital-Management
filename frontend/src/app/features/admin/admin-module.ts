import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { SharedModule } from '../../shared/shared-module';
import { AdminDoctors } from './doctors/admin-doctors';
import { AdminPatients } from './patients/admin-patients';

const routes: Routes = [
  { path: 'doctors',  component: AdminDoctors },
  { path: 'patients', component: AdminPatients },
  { path: '',         redirectTo: 'doctors', pathMatch: 'full' }
];

@NgModule({
  declarations: [
    AdminDoctors,
    AdminPatients
  ],
  imports: [SharedModule, RouterModule.forChild(routes)]
})
export class AdminModule {}
