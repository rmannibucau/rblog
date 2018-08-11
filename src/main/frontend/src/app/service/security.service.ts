import {Injectable, ReflectiveInjector} from '@angular/core';
import {Observable} from 'rxjs/Observable';
import {Observer} from 'rxjs/Observer';
import {RestClient} from './rest.service';

const HEADER = 'RBLOG-SECURITY-TOKEN'; // the actual http header name
const USERNAME = 'RBLOG-SECURITY.username';

@Injectable()
export class SecurityService {
    private username: string;
    private lifecycleEmitter: Observer<boolean>;
    lifecycleListener: Observable<boolean>;


    constructor(private http: RestClient) {
      const token = window.localStorage.getItem(HEADER);
      window.localStorage.getItem(USERNAME); // chrome workaround to have refreshes working
      if (token) {
          this.initToken(window.localStorage.getItem(USERNAME), token, false);
      }
      this.lifecycleListener = new Observable<boolean>(o => this.lifecycleEmitter = o)
          .startWith(this.internalIsLogged())
          .share();
    }

    initToken(username, token, store) {
        this.http.setHeader(HEADER, token);
        this.username = username;
        if (store) {
            window.localStorage.setItem(HEADER, token);
            window.localStorage.setItem(USERNAME, username);
        }
    }

    login(credentials) {
        return this.http.post('security/login', credentials)
            .map(res => {
              this.initToken(credentials.username, res.token, credentials.rememberMe)
              this.lifecycleEmitter.next(true);
              return res;
            });
    }

    logout() {
        return this.http.head('security/logout');
    }

    deleteTokens() {
        return this.http.head('security/empty');
    }

    invalidate() {
        this.username = null;
        window.localStorage.removeItem(HEADER);
        window.localStorage.removeItem(USERNAME);
        this.http.removeHeader(HEADER);
        this.lifecycleEmitter.next(false);
    }

    isLogged() {
        return !!this.username || this.internalIsLogged();
    }

    getUsername() {
        return !this.internalIsLogged() ? window.localStorage.getItem(USERNAME) || this.username : undefined;
    }

    private internalIsLogged() {
        return !!window.localStorage.getItem(HEADER);
    }
}
