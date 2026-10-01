import { NgModule } from '@angular/core';
import { SharedModule } from '../../shared/shared-module';
import { Login } from './login/login';
import { Register } from './register/register';

@NgModule({
  declarations: [Login, Register],
  imports: [SharedModule]
})
export class AuthModule {}
