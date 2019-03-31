import { BrowserModule } from '@angular/platform-browser';
import { NgModule } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { HttpClientModule, HTTP_INTERCEPTORS } from '@angular/common/http';
import { RouterModule, Routes } from '@angular/router';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome'

import { AppComponent } from './app.component';
import { HeaderComponent } from './components/shared/header/header.component';
import { FooterComponent } from './components/shared/footer/footer.component';
import { FormUploadComponent } from './components/upload/form-upload/form-upload.component';
import { DataTablesModule } from 'angular-datatables';
import { AddAdminComponent } from './components/add-admin/add-admin.component';
import { AddPmoComponent } from './components/add-pmo/add-pmo.component';
import { DatatablesLibraryComponent } from './components/datatables-library/datatables-library.component';
import { FeedbackComponent } from './components/feedback/feedback.component';
import { LoginPageComponent } from './login-page/login-page.component';
import { NeedAuthGuard } from './auth.guard';
import { NeedAdminGuard } from './admin.guard';
import { DashboardPageComponent } from './dashboard-page/dashboard-page.component';
import { LoginPageModule } from './login-page/login-page.module';
import { XhrInterceptor } from './interceptor/interceptor';
import { LogoutComponent } from './logout/logout.component';
import { FeedbackStatusComponent } from './components/feedback-status/feedback-status.component';


const routes: Routes = [
  { path: '', redirectTo: "login", pathMatch: "full" },
  { path: 'login', component: LoginPageComponent },
  { path: 'logout', component: LogoutComponent},
  { path: 'dashboard', component: DashboardPageComponent, canActivate: [NeedAuthGuard] },
  { path: 'feedback', component: FeedbackComponent },
  { path: 'upload-pmo', component: FormUploadComponent, canActivate: [NeedAdminGuard] },
  { path: 'add-admin', component: AddAdminComponent, canActivate: [NeedAdminGuard] },
  { path: 'add-pmo', component: AddPmoComponent, canActivate: [NeedAdminGuard] },
  { path: 'feedbackStatus', component: FeedbackStatusComponent, canActivate: [NeedAdminGuard] },
  { path: 'datatable', component: DatatablesLibraryComponent, canActivate: [NeedAuthGuard] },
];

@NgModule({
  declarations: [
    AppComponent,
    DashboardPageComponent,
    HeaderComponent,
    FooterComponent,
    FormUploadComponent,
    AddAdminComponent,
    AddPmoComponent,
    DatatablesLibraryComponent,
    FeedbackComponent,
    LogoutComponent,
    FeedbackStatusComponent
  ],
  imports: [
    BrowserModule,
    DataTablesModule,
    FontAwesomeModule,
    FormsModule,
    HttpClientModule,
    LoginPageModule,
    RouterModule.forRoot(routes)
  ],
  providers: [NeedAdminGuard, NeedAuthGuard, { provide: HTTP_INTERCEPTORS, useClass: XhrInterceptor, multi: true }],
  bootstrap: [AppComponent]
})
export class AppModule {
  constructor() {
    localStorage.clear();
  }
}
