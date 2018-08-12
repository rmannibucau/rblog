import {Injectable} from "@angular/core";
import {RestClient} from "./rest.service";

class QueryBuilder {
  private current: string;

  append(key, val) {
      if (!!val) {
          this.current = (this.current ? (this.current + '&') : '?') + key + '=' + val;
      }
      return this;
  }

  build() {
      return this.current ? this.current : '';
  }
}

@Injectable()
export class PostService {
    constructor(private http: RestClient) {
    }

    //
    // public
    //

    findPublicById(id) {
        return this.http.get('post/' + id);
    }

    findBySlug(slug) {
        return this.http.get('post/slug/' + slug);
    }

    search(request) {
        return this.http.get('post/select' + new QueryBuilder()
                .append('offset', request.offset)
                .append('number', request['number'])
                .append('orderBy', request.orderBy)
                .append('order', request.order)
                .append('type', request.type)
                .append('after', request.after)
                .append('before', request.before)
                .append('status', request.status)
                .append('search', request.search)
                .append('categorySlug', request.categorySlug)
                .append('categoryId', request.categoryId)
                .build());
    }

    top() {
        return this.http.get('post/top');
    }

    //
    // admin
    //

    findById(id) {
        return this.http.get('post/admin/' + id);
    }

    removeById(id) {
        return this.http.delete('post/admin/' + id);
    }

    save(post) {
        return this.http.post('post/admin', post);
    }
};
