import {Component, OnDestroy} from '@angular/core';
import {Router} from '@angular/router';
import {PostService} from '../../service/post.service';
import {PostList} from './component/post-list.component';
import {AnalyticsService} from '../../service/analytics.service';

@Component({
  selector: 'search',
  template: require('./search.pug'),
  directives: [PostList]
})
export class Search implements OnDestroy {
    notificationsOptions = {};
    title: string;
    searchOptions: any;

    private sub: any;

    constructor(private service: PostService,
                private analyticsService: AnalyticsService,
                private router: Router) {
      this.sub = this.router
        .routerState
        .queryParams
        .subscribe(params => {
          const query = params['query'];
          this.analyticsService.track('/search/' + query);
          this.title = 'Results for \'' + query + '\'';
          this.searchOptions = {search: query};
        });
    }

    ngOnDestroy() {
      this.sub.unsubscribe();
    }
}
