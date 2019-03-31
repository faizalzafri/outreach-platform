import { Component, OnInit } from '@angular/core';
import { faAngry, faFrown, faMeh, faSmile, faGrinAlt } from '@fortawesome/free-solid-svg-icons';
import { FeedbackService } from 'src/app/services/feedback.service';
import { ActivatedRoute } from '@angular/router';
import { Feedback } from 'src/app/models/Feedback';
import { Response } from 'src/app/models/Response'

@Component({
  selector: 'app-feedback',
  templateUrl: './feedback.component.html',
  styleUrls: ['./feedback.component.css']
})
export class FeedbackComponent implements OnInit {

  faAngry = faAngry;
  faFrown = faFrown;
  faMeh = faMeh;
  faSmile = faSmile;
  faGrinAlt = faGrinAlt;

  isangActive: boolean = false;
  isfroActive: boolean = false;
  ismehActive: boolean = false;
  issmiActive: boolean = false;
  isgriActive: boolean = false;

  message: string = "";
  submitted: boolean = false;

  feedback: Feedback = { eventId: "", employeeId: "", score: 0, answer1: "", answer2: "", status: "" };

  ques1: string;
  ques2: string;

  isHidden: boolean = true;
  response: Response = { message: "", httpStatus: 0 };

  constructor(private feedbackService: FeedbackService, private route: ActivatedRoute) { }

  ngOnInit() {

    // set feedback properties from query params
    this.feedback.eventId = this.route.snapshot.queryParamMap.get('event');
    this.feedback.employeeId = this.route.snapshot.queryParamMap.get('id');

    //load question from service
    //var questions = this.feedbackService.getQuestions();
    this.ques1 = 'Question 1';
    this.ques2 = 'Question 2';
  }

  iconAClicked(event: Event) {

    //reset color and color
    this.isfroActive = false;
    this.ismehActive = false;
    this.issmiActive = false;
    this.isgriActive = false;
    this.feedback.score = 0;

    //change color
    this.isangActive = !this.isangActive;

    //set score
    this.feedback.score = this.isangActive ? 1 : 0;

    console.log(this.feedback.score)

  }
  iconFClicked(event: Event) {

    //reset color and score
    this.ismehActive = false;
    this.issmiActive = false;
    this.isgriActive = false;
    this.isangActive = false;
    this.feedback.score = 0;

    //change color
    this.isfroActive = !this.isfroActive;

    //set score
    this.feedback.score = this.isfroActive ? 2 : 0;
    console.log(this.feedback.score)
  }
  iconMClicked(event: Event) {

    //reset color and score
    this.isfroActive = false;
    this.issmiActive = false;
    this.isgriActive = false;
    this.isangActive = false;
    this.feedback.score = 0;

    //change color
    this.ismehActive = !this.ismehActive;

    //set score
    this.feedback.score = this.ismehActive ? 3 : 0;
    console.log(this.feedback.score)
  }
  iconSClicked(event: Event) {

    //reset color and score
    this.isfroActive = false;
    this.ismehActive = false;
    this.isgriActive = false;
    this.isangActive = false;
    this.feedback.score = 0;

    //change color
    this.issmiActive = !this.issmiActive;

    //set score
    this.feedback.score = this.issmiActive ? 4 : 0;
    console.log(this.feedback.score)
  }
  iconGClicked(event: Event) {

    //reset color and score
    this.isangActive = false;
    this.isfroActive = false;
    this.ismehActive = false;
    this.issmiActive = false;
    this.feedback.score = 0;

    //change color
    this.isgriActive = !this.isgriActive;

    //set score
    this.feedback.score = this.isgriActive ? 5 : 0;
    console.log(this.feedback.score)
  }

  save() {

    this.feedbackService.saveFeedback(this.feedback)
      .subscribe((data: Response) => {
        this.response.message = data.message;
        this.response.httpStatus = data.httpStatus;
      }
      );

    if (this.response.httpStatus == 200) {
      console.log('hidden --')
      this.message = this.response.message;
      this.submitted = !this.submitted;
      this.isHidden = !this.isHidden;
      console.log('-- hidden --')
    }

  }


  onSubmit() {
    this.submitted = true;
    this.save();
  }

}
