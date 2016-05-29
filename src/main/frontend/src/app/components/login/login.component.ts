import {Component} from "@angular/core";
import {Router} from "@angular/router";
import {Observable} from "rxjs/Rx.d";
import {SecurityService} from "../../service/security.service";
import {RestClient} from "../../service/rest.service";

class LoginForm {
    username: string;
    password: string;
    rememberMe: boolean;
}

@Component({ selector: 'login', template: require('./login.pug') })
export class Login {
    formData: LoginForm = new LoginForm();
    error: string;

    constructor(private securityService: SecurityService,
                private router: Router) {
    }

    onSubmit() {
        this.securityService.login(this.formData)
          .subscribe(
            data => this.router.navigate(['/']),
            error => this.error = 'Error during login (HTTP ' + error.status + '): ' + (error.status == 400 ? error.json().message : error.text()));
    }
}


@Component({ selector: 'logout', template: require('./logout.pug') })
export class Logout {
    error: string;

    constructor(private securityService: SecurityService,
                private router: Router) {
        securityService.logout()
          .subscribe(
            data => router.navigate(['/']),
            error => this.error = 'Error during login (HTTP ' + error.status + '): ' + error.text(),
            () => securityService.invalidate());
    }
}
