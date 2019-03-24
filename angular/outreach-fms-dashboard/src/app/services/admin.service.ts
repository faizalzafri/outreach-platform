import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
@Injectable({
  providedIn: 'root'
})
export class AdminService {
  private baseUrl = "http://localhost:8090/feedback-server/api/";

  constructor(private http: HttpClient) { }

  createAdmin(id: string): Observable<string> {
    return this.http.post<string>(`${this.baseUrl}` + `addAdmin`, id);
  }
  createPMO(id: string): Observable<string> {
    return this.http.post<string>(`${this.baseUrl}` + `addPMO`, id);
  }

}
