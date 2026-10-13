import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { AuthService } from '../services/auth.service';

export const jwtInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);

  // Do not attach Authorization header to public auth endpoints
  const isAuthEndpoint =
    req.url.includes('/user/login') ||
    req.url.includes('/user/refresh-token') ||
    req.url.includes('/user/forgot-password');

  let requestToForward = req;

  if (!isAuthEndpoint) {
    const token = authService.getToken();
    if (token && !req.headers.has('Authorization')) {
      requestToForward = req.clone({
        setHeaders: {
          Authorization: `Bearer ${token}`
        }
      });
    }
  }

  return next(requestToForward).pipe(
    catchError((error: HttpErrorResponse) => {
      // Auto-logout if a protected API call returns 401 Unauthorized
      if (error.status === 401 && !isAuthEndpoint) {
        authService.logout();
      }
      return throwError(() => error);
    })
  );
};

