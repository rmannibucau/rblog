import {Injectable} from '@angular/core';
import {ResponseContentType} from '@angular/http';
import {RestClient} from './rest.service';

@Injectable()
export class BackupService {
    constructor(private http: RestClient) {
    }

    backup() {
        return this.http.getRaw('backup', ResponseContentType.ArrayBuffer);
    }
}
