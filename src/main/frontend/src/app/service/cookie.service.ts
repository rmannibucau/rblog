import { Cookie } from 'ng2-cookies/ng2-cookies';

export class DataProtectionLaw {
    private cookieName: string = 'rblog-data-low-protection';

    private callbacksOnAccept: Array<any> = [];
    accepted: boolean = false;

    constructor() {
        this.accepted = Cookie.get(this.cookieName) == 'accepted';
    }

    onAccepted(cb) {
        if (!this.accepted) {
            this.callbacksOnAccept.push(cb);
        } else {
            cb();
        }
    }

    onAccept() {
        this.callbacksOnAccept.forEach(c => c());
        Cookie.set(this.cookieName, "accepted");
        this.accepted = true;
    }

    onReject() { // don't set a cookie hoping user will change his mind next time
        this.accepted = false;
        this.callbacksOnAccept = [];
    }

    reset() {
        Cookie.delete(this.cookieName);
    }
}
