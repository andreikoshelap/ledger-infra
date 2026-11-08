import { HttpInterceptorFn } from '@angular/common/http';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const userId = localStorage.getItem('ledgerUserId') ?? '1';
  return next(req.clone({ setHeaders: { 'X-User-Id': userId } }));
};
