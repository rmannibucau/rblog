import {Injectable} from '@angular/core';
import {Http, Headers, RequestOptions} from '@angular/http';

const base = 'api/';

@Injectable()
export class RestClient {
    private postHeaders: Headers = new Headers();
    private sharedHeaders: Headers = new Headers();

    constructor(private http: Http) {
      this.postHeaders.append('Content-Type', 'application/json');
    }

    post(endpoint, body) {
        return this.http.post(base + endpoint, JSON.stringify(body), new RequestOptions({headers: this.postHeaders})).map(r => r.json());
    }

    head(endpoint) {
        return this.http.head(base + endpoint, new RequestOptions({headers: this.sharedHeaders}));
    }

    get(endpoint) {
        return this.http.get(base + endpoint, new RequestOptions({headers: this.sharedHeaders}))
        .map(r => {
            return r.json();
          });
    }

    delete(endpoint) {
        return this.http.delete(base + endpoint, new RequestOptions({headers: this.sharedHeaders}));
    }

    setHeader(key, value) {
      this.sharedHeaders.append(key, value);
      this.postHeaders.append(key, value);
    }

    removeHeader(key) {
      this.sharedHeaders.delete(key);
      this.postHeaders.delete(key);
    }
}
