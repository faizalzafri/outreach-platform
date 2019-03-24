import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { environment } from '../../environments/environment';
import { User } from '../interfaces/user';
import { LoginResponse } from '../interfaces/loginResponse';

@Injectable({
  providedIn: 'root'
})
export class AuthenticationService {

  private user: User;
  authenticated = false;
  userData: any = {};

  constructor(private http: HttpClient) {
  }

  private prepareHeader(headers: HttpHeaders | null, credentials: any): HttpHeaders {
    headers = headers || new HttpHeaders();
    headers.set('Authorization', 'Basic ' + btoa(credentials.username + ':' + credentials.password));
    console.log('Authorization', 'Basic ' + btoa(credentials.username + ':' + credentials.password))
    //headers = headers.set('Content-Type', 'application/x-www-form-urlencoded');
    // headers = headers.set('Access-Control-Allow-Origin', 'GET, POST, PUT, DELETE, *');
    return headers;
  }

  authenticate(credentials): boolean {

    // const headers = new HttpHeaders(credentials ? {
    //   authorization: 'Basic ' + btoa(credentials.username + ':' + credentials.password)
    // } : {});

    const headers = this.prepareHeader(null, credentials);
    const authServerUrl = environment.authServerUrl;

    this.http.get<LoginResponse>(authServerUrl + 'login', { headers: headers }).subscribe(
      loginResponseModel => {
        if (loginResponseModel.isUserAuthentic === true) {
          this.authenticated = true;
        }
      });

      if(this.authenticated){

        this.http.get<User>(authServerUrl + 'user', { headers: headers }).subscribe(
          user => {
            if (user.id === credentials.username) {
              this.setUser(user.id, user.name, user.emailId, user.role);
              this.setUserInLocalStrorage();
              this.authenticated = true;
            } else {
              this.authenticated = false;
            }
          });
      }
    
    return this.authenticated;
  }

  isLoggedIn(): boolean {

    return this.authenticated;
  }

  private setUser(id: string, name: string, email: string, role: string) {
    this.user = {
      id: id,
      name: name,
      emailId: email,
      role: role
    }
  }

  getUser(): User {
    return this.user;
  }

  clearStorage() {
    localStorage.clear();
  }

  getUserDetails(): User {
    let user: User = {
      id: localStorage.getItem("id"),
      name: localStorage.getItem("name"),
      emailId: localStorage.getItem("email"),
      role: localStorage.getItem("role"),
    }
    return user;
  }

  private setUserInLocalStrorage() {
    localStorage.setItem("name", this.user.name);
    localStorage.setItem("email", this.user.emailId);
    localStorage.setItem("id", this.user.id);
    localStorage.setItem("role", this.user.role);
    localStorage.setItem("user_authentic", "" + this.authenticated);
  }
}
