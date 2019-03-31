import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { FeedbackStatus } from '../models/FeedbackStatus';
import { environment } from 'src/environments/environment';
@Injectable({
  providedIn: 'root'
})
export class AdminService {

  private baseUrl = environment.feedbackServerUrl;

  constructor(private http: HttpClient) { }

  isAdmin(): boolean {
    var role = localStorage.getItem("role");
    console.log(role)
    return role == "ROLE_ADMIN";

  }
  createAdmin(id: string): Observable<string> {
    return this.http.post<string>(`${this.baseUrl}` + `api/addAdmin`, id);
  }
  createPMO(id: string): Observable<string> {
    return this.http.post<string>(`${this.baseUrl}` + `api/addPMO`, id);
  }

  getReport(selectedValue: string, ascid: string): any {
    return this.http.get<FeedbackStatus[]>(this.baseUrl + 'api/report', { headers: { "ascid": ascid } });
  }

}
