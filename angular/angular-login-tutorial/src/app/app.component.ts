import { Component } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';


@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css']
})
export class AppComponent {
  pageTitle = 'Outreach Feedback Management System';

  // constructor(private app: ApiService, private http: HttpClient, private router: Router) {
  //   this.app.authenticate(undefined, undefined);
  // }
  // logout() {
  //   this.http.post('logout', {}).subscribe(() => {
  //       this.app.authenticated = false;
  //       this.router.navigateByUrl('/login');
  //   });
  // }
}
