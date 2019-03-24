import { Injectable } from "@angular/core";
import { HttpInterceptor, HttpRequest, HttpHandler } from "@angular/common/http";

@Injectable()
export class XhrInterceptor implements HttpInterceptor {

  intercept(req: HttpRequest<any>, next: HttpHandler) {

    console.log('Entering Intercept')

    const xhr = req.clone({
      headers: req.headers.set('X-Requested-With', 'XMLHttpRequest').set('Authorization', 'Basic dXNlcjp1c2Vy')
    });

    console.log('Exiting Intercept')

    return next.handle(xhr);
  }
}