import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { AuthenticationService } from '../services/authentication.service';

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

    console.log('entering login component ngInit');

    if (this.authService.isLoggedIn()) {
      console.log('login component');
      this.router.navigate(['/dashboard']);
    }

    console.log('exiting login component ngInit');

  }

  login() {

    console.log('entering login component login');

    localStorage.clear();

    this.authenticated = this.authService.authenticate(this.credentials);

    if (this.authenticated) {
      this.router.navigate(['/dashboard']);
    } else {
      this.message = "Bad Credentails";
    }

    console.log('exiting login component login');

    return false;
  }
}
