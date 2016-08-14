import {Injectable} from '@angular/core';
import {Observable} from 'rxjs/Observable';
import {RestClient} from './rest.service';

@Injectable()
export class BitlyService {
  private active: boolean = null;

  constructor(private http: RestClient) {
  }

  isActive() {
    if (this.active != null) {
      return Observable.of(this.active);
    }
    return this.http.get('bitly/state').map(json => {
      this.active = !!json['active'];
      return this.active;
    });
  }

  shorten(url) {
    return this.http.post('bitly/shorten', {value: url}).map(json => json['value']);
  }
}
