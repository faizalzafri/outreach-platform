import { NgModule } from '@angular/core';
import { Routes, RouterModule } from '@angular/router';
import { FormUploadComponent } from './components/upload/form-upload/form-upload.component';
import { AddAdminComponent } from './components/add-admin/add-admin.component';
import { AddPmoComponent } from './components/add-pmo/add-pmo.component';
import { DatatablesLibraryComponent } from './components/datatables-library/datatables-library.component';
import { FeedbackComponent } from './components/feedback/feedback.component';

const routes: Routes = [
  { path: 'feedback', component: FeedbackComponent },
  { path: 'upload-pmo', component: FormUploadComponent },
  { path: 'add-admin', component: AddAdminComponent },
  { path: 'add-pmo', component: AddPmoComponent },
  { path: 'datatable', component: DatatablesLibraryComponent },
  {
    path: '',
    redirectTo: '',
    pathMatch: 'full'
  }];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule { }
