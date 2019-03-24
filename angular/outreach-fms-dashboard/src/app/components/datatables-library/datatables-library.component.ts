import { Component, OnInit, OnChanges, SimpleChanges, ViewChild, AfterViewInit, OnDestroy } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { EventReport } from 'src/app/models/EventReport';
import { FeedbackService } from 'src/app/services/feedback.service';
import { Subject } from 'rxjs';
import { DataTableDirective } from 'angular-datatables';

@Component({
  selector: 'app-datatableslibrary',
  templateUrl: './datatables-library.component.html',
  styleUrls: ['./datatables-library.component.css']
})
export class DatatablesLibraryComponent implements AfterViewInit, OnDestroy, OnInit {

  selectedValue: string = '';
  eventReport: EventReport[];
  check: boolean = true;

  @ViewChild(DataTableDirective)
  dtElement: DataTableDirective;
  dtOptions: any = {};
  dtTrigger: Subject<any> = new Subject<any>();

  constructor(private http: HttpClient, private feedbackService: FeedbackService) {
  }

  selectChangeHandler(event: any) {

    this.selectedValue = event.target.value;

    this.dtElement.dtInstance.then((dtInstance: DataTables.Api) => {
      // Destroy the table first
      dtInstance.destroy();
      // Call the dtTrigger to rerender again
    });

    console.log(this.selectedValue);
    this.feedbackService.getReport2(this.selectedValue).subscribe(data => {
      console.log(data);
      this.eventReport = data;
      this.dtTrigger.next();
    });
  }


  ngOnInit() {

    this.dtOptions = {
      pagingType: 'full_numbers',
      pageLength: 5,
      processing: true,
      dom: 'Bfrtip',
      buttons: [
        'copy', 'csv', 'excel', 'pdf', 'print'
      ],

    }
  }

  ngAfterViewInit(): void {
    this.dtTrigger.next();
  }

  ngOnDestroy(): void {
    this.dtTrigger.unsubscribe();
  }

}