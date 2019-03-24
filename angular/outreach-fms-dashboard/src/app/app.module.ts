import { BrowserModule } from '@angular/platform-browser';
import { NgModule } from '@angular/core';

import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
import { HeaderComponent } from './components/shared/header/header.component';
import { FooterComponent } from './components/shared/footer/footer.component';
import { FormUploadComponent } from './components/upload/form-upload/form-upload.component';
import {HttpClientModule} from '@angular/common/http';
import { DataTablesModule } from 'angular-datatables';
import { AddAdminComponent } from './components/add-admin/add-admin.component';
import { FeedbackScore } from './models/FeedbackScore';
import { AddPmoComponent } from './components/add-pmo/add-pmo.component';
import { DatatablesLibraryComponent } from './components/datatables-library/datatables-library.component';
import { FormsModule } from '@angular/forms';
import { FeedbackComponent } from './components/feedback/feedback.component';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';


@NgModule({
  declarations: [
    AppComponent,
    HeaderComponent,
    FooterComponent,
    FormUploadComponent,
    AddAdminComponent,
    AddPmoComponent,
    DatatablesLibraryComponent,
    FeedbackComponent
  ],
  imports: [
    BrowserModule,
    AppRoutingModule,
    HttpClientModule,
    DataTablesModule,
    FormsModule,
    FontAwesomeModule
  ],
  providers: [FeedbackScore],
  bootstrap: [AppComponent]
})
export class AppModule { }
