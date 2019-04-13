import {Injectable, EventEmitter} from '@angular/core';
import {map} from 'rxjs/operators'
import {RestClient} from "./rest.service";

@Injectable()
export class CategoryService {
    private listener: EventEmitter<void> = new EventEmitter<void>();

    constructor(private http: RestClient) {
    }

    findRoots() {
        return this.http.get('category/roots');
    }

    findAll() {
        return this.http.get('category/all');
    }

    findById(id) {
        return this.http.get('category/' + id);
    }

    findBySlug(slug) {
        return this.http.get('category/slug/' + slug);
    }

    save(category) {
        return this.http.post('category', category).pipe(map(res => {
          this.listener.emit(null);
          return res;
        }));
    }

    removeById(id) {
        return this.http.delete('category/' + id).pipe(map(r => {
          this.listener.emit(null);
          return r;
        }));
    }

    listenChanges(subscriber) {
      this.listener.subscribe(subscriber);
    }
};
