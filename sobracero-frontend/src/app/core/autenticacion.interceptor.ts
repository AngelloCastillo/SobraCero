import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { environment } from '../../environments/environment';
import { AutenticacionService } from './autenticacion.service';

/** Agrega el JWT a las llamadas al backend y cierra la sesión si expira (401). */
export const autenticacionInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AutenticacionService);
  const token = auth.token();
  const esApi = req.url.startsWith(environment.apiUrl);
  const conToken = token && esApi ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } }) : req;
  return next(conToken).pipe(
    catchError((e: HttpErrorResponse) => {
      if (e.status === 401 && token && !req.url.includes('/api/autenticacion/')) auth.cerrarSesion();
      return throwError(() => e);
    }),
  );
};
