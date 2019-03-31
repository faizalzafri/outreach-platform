import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { AuthenticationService } from '../services/authentication.service';
import { User } from '../models/user';

@Component({
  selector: 'app-login-page',
  templateUrl: './login-page.component.html',
  styleUrls: ['./login-page.component.css']
})
export class LoginPageComponent implements OnInit {

  credentials = { username: "", password: "" };
  authenticated: boolean = false;
  message: String = "";

  constructor(private authService: AuthenticationService, private router: Router) {

  }

  ngOnInit() {
    if (this.authService.isLoggedIn()) {
      this.router.navigate(['/dashboard']);
    }
  }

  login() {
    this.saveCredentialsToLocal();
    this.authenticateLogin();
  }

  private authenticateLogin() {
    this.authService.authenticate()
      .subscribe(

        (user: User) => {
          if (user.isUserAuthentic == "true") {
            this.authenticated = user.isUserAuthentic == "true";
            this.authService.setLoggedIn(this.authenticated);
            this.authService.setUser(user.id, user.name, user.emailId, user.role, user.isUserAuthentic);
          }

          if (this.authenticated) {
            this.router.navigate(['/dashboard']);
          }
          else {
            this.message = "Bad Credentails";
            this.router.navigate(['/login']);
          }
        });
  }

  private saveCredentialsToLocal() {
    localStorage.setItem('username', this.credentials.username);
    localStorage.setItem('password', this.credentials.password);
  }
}

