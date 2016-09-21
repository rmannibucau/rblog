import {Injectable} from '@angular/core';
import {DataProtectionLaw} from './cookie.service';

@Injectable()
export class Twitter {
  constructor(private dpl: DataProtectionLaw) {
  }

  lazyLoad() {
    this.dpl.onAccepted(() => {
      !function(d,s,id){
        var js,fjs=d.getElementsByTagName(s)[0],p=/^http:/.test(d.location.protocol)?'http':'https';
        if(!d.getElementById(id)) {
          js=d.createElement(s);
          js.id=id;
          js.src=p+'://platform.twitter.com/widgets.js';
          fjs.parentNode.insertBefore(js,fjs);
        } else {
          window['twttr']['widgets']['load']();
        }
      }(document, 'script', 'twitter-wjs');
    });
  }
}
