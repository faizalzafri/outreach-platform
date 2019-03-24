import { Component,ViewChild,OnInit } from '@angular/core';
declare var $;
@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css']
})
export class AppComponent implements OnInit{

  title = 'JavaSampleApproach';
  description = 'Angular-SpringBoot';

  @ViewChild('dataTable') table;
  dataTable: any;
  dtOptions: any;
  constructor(){
  }

  ngOnInit(): void {
    this.dtOptions = {
        "ajax": {
          url: 'http://localhost/country/country.php',
          type: 'POST',
            data: {'id': 1, 'name': 'sathish'}
        },
        columns: [
            {
              title: 'Short Name',
                data: 'short_name'
            },
            {
                title: 'Long Name',
                data: 'long_name'
            },
            {
                title: 'ISO 3',
                data: 'iso3'
            },
            {
                title: 'ISO 2',
                data: 'iso2'
            }
        ],
        dom: 'Bfrtip',
        buttons: [
            'copy', 'csv', 'excel', 'pdf', 'print'
        ]
    };
    this.dataTable = $(this.table.nativeElement);
    this.dataTable.DataTable(this.dtOptions);
  }
}