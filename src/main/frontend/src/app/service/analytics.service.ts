import {Injectable, EventEmitter} from '@angular/core';
import {map} from 'rxjs/operators'
import {RestClient} from './rest.service';
import {DataProtectionLaw} from './cookie.service';
import {SecurityService} from './security.service';

@Injectable()
export class AnalyticsService {
  private code: string;
  private pages: Array<string> = [];

  constructor(private http: RestClient,
              private dpl: DataProtectionLaw,
              private securityService: SecurityService) {
      if (this.isSkipped()) {
        return;
      }
      this.dpl.onAccepted(() => {
          http.get('configuration')
        .pipe(map(r => r.analytics))
        .subscribe(code => {
          if (!code) {
            return;
          }

          // load google analytics synchronously
          (function(i,s,o,g,r,a,m){i['GoogleAnalyticsObject']=r;i[r]=i[r]||function(){
          (i[r].q=i[r].q||[]).push(arguments)},i[r].l=new Date().getTime();a=s.createElement(o),
          m=s.getElementsByTagName(o)[0];a.src=g;m.parentNode.insertBefore(a,m)
          })(window,document,'script','//www.google-analytics.com/analytics.js','ga', null, null);

          // create a tracker
          window['ga']('create', code, 'auto');

          // now it is loaded and this.ga is valorised add stats about viewed pages
          this.pages.forEach(p => this.track(p));
          this.pages = [];
        }, e => {});
      });
    }

  track(page) {
    if (this.isSkipped()) {
      return;
    }
    if (!window['ga']) {
      if (this.pages.indexOf(page) < 0) {
        this.pages.push(page);
      }
      return;
    }
    window['ga']('set', 'page', page);
    window['ga']('send', 'pageview');
  }

  private isSkipped() { // we don't want to track the activity of admins
    return this.securityService.isLogged();
  }
}
