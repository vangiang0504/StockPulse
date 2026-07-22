import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { NotificationService } from '../services/notification.service';

/**
 * Blocks a route unless the signed-in user holds one of `allowedRoles`.
 *
 * This mirrors the `@PreAuthorize` rules on the backend controllers. Without it a
 * STAFF user can still reach a create/edit form by typing the URL, fill it in, and
 * only discover the restriction when the submit comes back as 403.
 *
 * Usage: `canActivate: [roleGuard('MANAGER', 'ADMIN')]`
 */
export const roleGuard = (...allowedRoles: string[]): CanActivateFn => () => {
  const router = inject(Router);
  const auth = inject(AuthService);
  const notification = inject(NotificationService);

  const role = auth.getRole();
  if (role && allowedRoles.includes(role)) {
    return true;
  }

  notification.error('You do not have permission to open that page');
  return router.createUrlTree(['/dashboard']);
};
