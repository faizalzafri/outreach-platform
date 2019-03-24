import { Component, OnInit } from '@angular/core';
import { AdminService } from 'src/app/services/admin.service';

@Component({
  selector: 'app-add-admin',
  templateUrl: './add-admin.component.html',
  styleUrls: ['./add-admin.component.css']
})
export class AddAdminComponent implements OnInit {

  id:string;
  submitted=false;
  message:string;

  constructor(private adminService:AdminService) { }

  ngOnInit() {
  }

  newCustomer(): void {
    this.submitted = false;
  }

  save() {
   this.adminService.createAdmin(this.id)
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
