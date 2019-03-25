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
  faSmile = faSmile;
  faGrinAlt = faGrinAlt;

  isangActive: boolean = false;
  isfroActive: boolean = false;
  ismehActive: boolean = false;
  issmiActive: boolean = false;
  isgriActive: boolean = false;

  score: number = 0;

  constructor() { }

  ngOnInit() {
  }

  iconAClicked(event: Event) {

    //reset color and color
    this.isfroActive = false;
    this.ismehActive = false;
    this.issmiActive = false;
    this.isgriActive = false;
    this.score = 0;
    
    //change color
    this.isangActive = !this.isangActive;

    //set score
    this.score = this.isangActive ? 1 : 0;

    console.log(this.score)

  }
  iconFClicked(event: Event) {

    //reset color and score
    this.ismehActive = false;
    this.issmiActive = false;
    this.isgriActive = false;
    this.isangActive = false;
    this.score = 0;

    //change color
    this.isfroActive = !this.isfroActive;

    //set score
    this.score = this.isfroActive ? 2 : 0;
    console.log(this.score)
  }
  iconMClicked(event: Event) {

    //reset color and score
    this.isfroActive = false;
    this.issmiActive = false;
    this.isgriActive = false;
    this.isangActive = false;
    this.score = 0;

    //change color
    this.ismehActive = !this.ismehActive;

    //set score
    this.score = this.ismehActive ? 3 : 0;
    console.log(this.score)
  }
  iconSClicked(event: Event) {

    //reset color and score
    this.isfroActive = false;
    this.ismehActive = false;
    this.isgriActive = false;
    this.isangActive = false;
    this.score = 0;

    //change color
    this.issmiActive = !this.issmiActive;

    //set score
    this.score = this.issmiActive ? 4 : 0;
    console.log(this.score)
  }
  iconGClicked(event: Event) {

    //reset color and score
    this.isangActive = false;
    this.isfroActive = false;
    this.ismehActive = false;
    this.issmiActive = false;
    this.score = 0;

    //change color
    this.isgriActive = !this.isgriActive;

    //set score
    this.score = this.isgriActive ? 5 : 0;
    console.log(this.score)
  }


}
