import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { NavBar } from './components/nav-bar/nav-bar';
import { ToastComponent } from './components/toast/toast';

@NgModule({
  declarations: [NavBar, ToastComponent],
  imports: [CommonModule, FormsModule, RouterModule],
  exports: [CommonModule, FormsModule, RouterModule, NavBar, ToastComponent]
})
export class SharedModule {}
