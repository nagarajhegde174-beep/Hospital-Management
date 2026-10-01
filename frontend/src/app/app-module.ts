import { NgModule, provideBrowserGlobalErrorListeners } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { AppRoutingModule } from './app-routing-module';
import { App } from './app';
import { CoreModule } from './core/core.module';
import { AuthModule } from './features/auth/auth-module';
import { SharedModule } from './shared/shared-module';
import { NotFound } from './features/not-found/not-found';
import { Landing } from './features/landing/landing';

@NgModule({
  declarations: [App, NotFound, Landing],
  imports: [
    BrowserModule,
    AppRoutingModule,
    CoreModule,
    SharedModule,
    AuthModule
  ],
  providers: [
    provideBrowserGlobalErrorListeners()
  ],
  bootstrap: [App]
})
export class AppModule {}
