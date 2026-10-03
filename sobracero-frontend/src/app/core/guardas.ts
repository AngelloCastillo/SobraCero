import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AutenticacionService } from './autenticacion.service';
import { Rol } from './modelos';

/** Exige sesión iniciada y, opcionalmente, uno de los roles indicados. */
export const guardaRol = (...roles: Rol[]): CanActivateFn => () => {
  const auth = inject(AutenticacionService);
  const router = inject(Router);
  const rol = auth.rol();
  if (!rol) return router.createUrlTree(['/login']);
  if (roles.length && !roles.includes(rol)) return router.createUrlTree([auth.inicioPara(rol)]);
  return true;
};
