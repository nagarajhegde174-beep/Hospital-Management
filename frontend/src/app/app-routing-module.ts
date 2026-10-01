import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { Login } from './features/auth/login/login';
import { Register } from './features/auth/register/register';
import { Landing } from './features/landing/landing';
import { NotFound } from './features/not-found/not-found';
import { AuthGuard } from './core/auth/auth.guard';
import { RoleGuard } from './core/auth/role.guard';

const routes: Routes = [
  { path: '', component: Landing, pathMatch: 'full' },
  { path: 'login', component: Login },
  { path: 'register', component: Register },
  {
    path: 'patient',
    canActivate: [AuthGuard, RoleGuard],
    data: { role: 'ROLE_PATIENT' },
    loadChildren: () =>
      import('./features/patient/patient-module').then(m => m.PatientModule)
  },
  {
    path: 'doctor',
    canActivate: [AuthGuard, RoleGuard],
    data: { role: 'ROLE_DOCTOR' },
    loadChildren: () =>
      import('./features/doctor/doctor-module').then(m => m.DoctorModule)
  },
  {
    path: 'admin',
    canActivate: [AuthGuard, RoleGuard],
    data: { role: 'ROLE_ADMIN' },
    loadChildren: () =>
      import('./features/admin/admin-module').then(m => m.AdminModule)
  },
  { path: '**', component: NotFound }
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule {}
