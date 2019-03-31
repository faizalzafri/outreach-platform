import { Injectable } from '@angular/core';
import { EventReport } from '../models/EventReport';
import { Observable } from 'rxjs';
import { HttpClient } from '@angular/common/http';
import { Feedback } from '../models/Feedback';
import { Response } from 'src/app/models/Response';
import { environment } from 'src/environments/environment';

@Injectable({
  providedIn: 'root'
})
export class FeedbackService {

  private feedServerbackUrl = environment.feedbackServerUrl;
  response: Response;

  constructor(private http: HttpClient) { }

  getReport(selectedValue: string, ascid: string): any {
    return this.http.get<EventReport[]>(this.feedServerbackUrl + 'report/' + selectedValue, { headers: { "ascid": ascid } });
  }

  saveFeedback(feedback: Feedback): Observable<any> {
    return this.http.post<Response>(this.feedServerbackUrl + 'feedback', feedback);
  }

}
