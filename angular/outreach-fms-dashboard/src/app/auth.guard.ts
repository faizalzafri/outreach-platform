import { CanActivate } from '@angular/router';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ActivatedRouteSnapshot, RouterStateSnapshot } from '@angular/router/src/router_state';
import { AuthenticationService } from './services/authentication.service';

@Injectable()
export class NeedAuthGuard implements CanActivate {

  constructor(private authService: AuthenticationService) {
  }

  canActivate(
    next: ActivatedRouteSnapshot,
    state: RouterStateSnapshot): Observable<boolean> | Promise<boolean> | boolean {
    if (this.authService.isLoggedIn())
      return true;
    else {
      localStorage.clear();
      return false;
    }
  }
}
