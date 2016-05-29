import {Injectable, EventEmitter} from "@angular/core";
import {RestClient} from "./rest.service";

@Injectable()
export class UserService {
    constructor(private http: RestClient) {
    }

    findById(id) {
        return this.http.get('user/' + id);
    }

    save(user) {
        return this.http.post('user', user);
    }

    removeById(id) {
        return this.http.delete('user/' + id);
    }

    findAll() {
        return this.http.get('user');
    }
}
