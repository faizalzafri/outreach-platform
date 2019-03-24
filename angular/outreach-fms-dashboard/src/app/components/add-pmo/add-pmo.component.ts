import { Component, OnInit } from '@angular/core';
import {AdminService} from '../../services/admin.service';

@Component({
  selector: 'app-add-pmo',
  templateUrl: './add-pmo.component.html',
  styleUrls: ['./add-pmo.component.css']
})
export class AddPmoComponent implements OnInit {

  id:string;
  submitted=false;
  message:string='';

  constructor(private adminService:AdminService) { }

  ngOnInit() {
  }

  save() {
   this.adminService.createPMO(this.id)
     .subscribe(data => {
       console.log("data: "+data)
        if(data=='0'){
          this.message="Id Already Exists";
        }else{
          this.message="Updated Successfully"
        }
        this.id="";
      }, error => {
        console.log("Error :"+error);
        this.message="Error Occurred";
        this.id="";
    });
  }

  onSubmit() {
    this.submitted = true;
    this.save();
  }
}
