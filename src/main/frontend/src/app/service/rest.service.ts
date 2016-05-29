import {Injectable} from '@angular/core';
import {Http, Headers} from '@angular/http';

const base = 'api/';

@Injectable()
export class RestClient {
    private headers: Headers = new Headers();

    constructor(private http: Http) {
      this.headers.append('Content-Type', 'application/json');
    }

    post(endpoint, body) {
        return this.http.post(base + endpoint, JSON.stringify(body), {headers: this.headers}).map(r => r.json());
    }

    head(endpoint) {
        return this.http.head(base + endpoint, {headers: this.headers});
    }

    get(endpoint) {
        return this.http.get(base + endpoint, {headers: this.headers}).map(r => r.json());
    }

    delete(endpoint) {
        return this.http.delete(base + endpoint, {headers: this.headers});
    }

    setHeader(key, value) {
      this.headers.append(key, value);
    }

    removeHeader(key) {
      this.headers.delete(key);
    }
}
