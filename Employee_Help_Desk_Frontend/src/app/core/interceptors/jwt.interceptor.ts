import { HttpInterceptorFn, HttpRequest, HttpHandlerFn, HttpEvent, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { Observable, catchError, filter, switchMap, take, throwError } from 'rxjs';
import { AuthService } from '../services/auth.service';

export const jwtInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);

  // Do not attach Authorization header to public auth endpoints
  const isAuthEndpoint =
    req.url.includes('/user/login') ||
    req.url.includes('/user/refresh-token') ||
    req.url.includes('/user/forgot-password');

  let requestToForward = req.clone({
    withCredentials: true
  });

  if (!isAuthEndpoint) {
    const token = authService.getToken();
    if (token && !req.headers.has('Authorization')) {
      requestToForward = requestToForward.clone({
        setHeaders: {
          Authorization: `Bearer ${token}`
        }
      });
    }
  }

  return next(requestToForward).pipe(
    catchError((error: HttpErrorResponse) => {
      // Refresh token if a protected API call returns 401 Unauthorized
      if (error.status === 401 && !isAuthEndpoint) {
        return handle401Error(req, next, authService, error);
      }
      return throwError(() => error);
    })
  );
};

function handle401Error(
  req: HttpRequest<unknown>,
  next: HttpHandlerFn,
  authService: AuthService,
  originalError: HttpErrorResponse
): Observable<HttpEvent<unknown>> {
  if (typeof authService.refreshToken !== 'function') {
    authService.logout();
    return throwError(() => originalError);
  }

  if (!authService.isRefreshing) {
    authService.isRefreshing = true;
    authService.refreshTokenSubject.next(null);

    return authService.refreshToken().pipe(
      switchMap((response) => {
        authService.isRefreshing = false;
        authService.refreshTokenSubject.next(response.accessToken);
        return next(
          req.clone({
            withCredentials: true,
            setHeaders: {
              Authorization: `Bearer ${response.accessToken}`
            }
          })
        );
      }),
      catchError((refreshErr) => {
        authService.isRefreshing = false;
        authService.logout();
        return throwError(() => refreshErr);
      })
    );
  } else {
    // If refresh is already in progress, wait for the new access token and retry
    return authService.refreshTokenSubject.pipe(
      filter((token): token is string => token !== null),
      take(1),
      switchMap((newToken) => {
        return next(
          req.clone({
            withCredentials: true,
            setHeaders: {
              Authorization: `Bearer ${newToken}`
            }
          })
        );
      })
    );
  }
}
