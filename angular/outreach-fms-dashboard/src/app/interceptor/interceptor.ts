import { Injectable } from "@angular/core";
import { HttpInterceptor, HttpRequest, HttpHandler } from "@angular/common/http";

@Injectable()
export class XhrInterceptor implements HttpInterceptor {

  intercept(req: HttpRequest<any>, next: HttpHandler) {
    var usr = localStorage.getItem('username')
    var pwd = localStorage.getItem('password')

    console.log('Entering Intercept')
    
    const xhr = req.clone({
      headers: req.headers.set('X-Requested-With', 'XMLHttpRequest')
      .set('Authorization', 'Basic ' + btoa(usr + ':' + pwd))
      
    });

    console.log(req.headers)
    console.log('Exiting Intercept')

    return next.handle(xhr);
  }
}