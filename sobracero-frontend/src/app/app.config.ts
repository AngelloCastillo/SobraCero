import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { ApplicationConfig, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideRouter, withHashLocation } from '@angular/router';
import { rutas } from './app.routes';
import { autenticacionInterceptor } from './core/autenticacion.interceptor';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(rutas, withHashLocation()),   // rutas con "#/" para funcionar en cualquier servidor estático
    provideHttpClient(withInterceptors([autenticacionInterceptor])),
  ],
};
