import {Injectable} from '@angular/core';
import {DataProtectionLaw} from './cookie.service';

function lazyLoadFn(d,s,id) {
  var js;
  var fjs=d.getElementsByTagName(s)[0];
  var p=/^http:/.test(d.location.protocol)?'http':'https';
  if(!d.getElementById(id)) {
    js=d.createElement(s);
    js.id=id;
    js.src=p+'://platform.twitter.com/widgets.js';
    fjs.parentNode.insertBefore(js,fjs);
  } else {
    window['twttr']['widgets']['load']();
  }
}

@Injectable()
export class Twitter {
  constructor(private dpl: DataProtectionLaw) {
  }

  lazyLoad() {
    this.dpl.onAccepted(() => lazyLoadFn(document, 'script', 'twitter-wjs'));
  }
}
