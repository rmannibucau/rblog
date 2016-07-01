import {Component, OnInit} from '@angular/core';
import {ActivatedRoute} from '@angular/router';
import {PostService} from '../../service/post.service';
import {PostList} from './component/post-list.component';
import {AnalyticsService} from '../../service/analytics.service';

@Component({
  selector: 'search',
  template: require('./search.pug'),
  directives: [PostList]
})
export class Search implements OnInit {
    notificationsOptions = {};
    title: string;
    searchOptions: any;

    private sub: any;

    constructor(private service: PostService,
                private analyticsService: AnalyticsService,
                private route: ActivatedRoute) {
    }

    ngOnInit() {
      const query = this.route.snapshot.params['query'];
      this.analyticsService.track('/search/' + query);
      this.title = 'Results for \'' + query + '\'';
      this.searchOptions = {search: query};
    }
}
