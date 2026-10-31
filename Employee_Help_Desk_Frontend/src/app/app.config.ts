import { ApplicationConfig, APP_INITIALIZER, provideBrowserGlobalErrorListeners } from '@angular/core'; // add APP_INITIALIZER
import { provideRouter } from '@angular/router';
import { routes } from './app.routes';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { jwtInterceptor } from './core/interceptors/jwt.interceptor';
import { AuthService } from './core/services/auth.service';
import { firstValueFrom, of } from 'rxjs';
import { catchError } from 'rxjs/operators';

export function initializeApp(authService: AuthService) {
  return () => {
    // Only attempt refresh if a session was previously active
    if (localStorage.getItem('ehd_employee_id')) {
      return firstValueFrom(
        authService.refreshToken().pipe(
          catchError(() => of(null)) // If refresh fails or expired, user will navigate to /login
        )
      );
    }
    return Promise.resolve();
  };
}

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes),
    provideHttpClient(withInterceptors([jwtInterceptor])),
    {
      provide: APP_INITIALIZER,
      useFactory: initializeApp,
      deps: [AuthService],
      multi: true
    }
  ]
};
