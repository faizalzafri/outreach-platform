import { Component, OnInit } from '@angular/core';
import { faAngry, faFrown, faMeh, faSmile, faGrinAlt } from '@fortawesome/free-solid-svg-icons';

@Component({
  selector: 'app-feedback',
  templateUrl: './feedback.component.html',
  styleUrls: ['./feedback.component.css']
})
export class FeedbackComponent implements OnInit {

  faAngry = faAngry;
  faFrown = faFrown;
  faMeh = faMeh;
  faSmile =faSmile;
  faGrinAlt = faGrinAlt;


  constructor() { }

  ngOnInit() {
  }

}
