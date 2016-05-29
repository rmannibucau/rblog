import {Injectable, EventEmitter} from "@angular/core";
import {RestClient} from "./rest.service";
import {DataProtectionLaw} from './cookie.service';

@Injectable()
export class AnalyticsService {
  private code: string;
  private pages: Array<string> = [];

  constructor(private http: RestClient,
              private dpl: DataProtectionLaw) {
      this.dpl.onAccepted(() => {
        http.get('configuration')
        .map(r => r.analytics)
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
      if (!window['ga']) {
        if (this.pages.indexOf(page) < 0) {
          this.pages.push(page);
        }
        return;
      }
      window['ga']('set', 'page', page);
      window['ga']('send', 'pageview');
    }
  }
