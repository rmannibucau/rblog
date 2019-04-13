import {Component, OnInit, OnDestroy} from '@angular/core';
import {ActivatedRoute} from '@angular/router';
import {PostService} from '../../service/post.service';
import {AnalyticsService} from '../../service/analytics.service';

@Component({
  selector: 'search',
  template: require('./search.pug')()
})
export class Search implements OnInit, OnDestroy {
    notificationsOptions = {};
    title: string;
    searchOptions: any;

    private sub: any;

    constructor(private service: PostService,
                private analyticsService: AnalyticsService,
                private route: ActivatedRoute) {
    }

    ngOnInit() {
      this.sub = this.route.params.subscribe(params => {
        const query = params['query'];
        this.analyticsService.track('/search/' + query);
        this.title = 'Results for \'' + query + '\'';
        this.searchOptions = {search: query};
      });
    }

    ngOnDestroy() {
      if (this.sub) {
        this.sub.unsubscribe();
      }
    }
}
