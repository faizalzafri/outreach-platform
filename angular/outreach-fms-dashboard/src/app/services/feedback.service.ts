import { Injectable } from '@angular/core';
import { EventReport } from '../models/EventReport';
import { Observable } from 'rxjs';
import { HttpClient } from '@angular/common/http';

@Injectable({
  providedIn: 'root'
})
export class FeedbackService {

  private url: string = "http://localhost:4200/assets/events/events.json";
  private url2: string = "http://localhost:8090/feedback-server/report/";
  constructor(private http: HttpClient) { }

  getReport(selectedValue: any): Observable<EventReport[]> {
    return this.http.get<EventReport[]>(this.url);
  }

  getReport2(selectedValue: string): any {

    var tempUrl = this.url2.concat(selectedValue);
    console.log(tempUrl);
    return this.http.get<EventReport[]>(tempUrl);
  }

}
