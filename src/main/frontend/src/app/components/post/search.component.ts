import {Component} from '@angular/core';
import {OnActivate, RouteSegment, RouteTree} from '@angular/router';
import {PostService} from '../../service/post.service';
import {PostList} from './component/post-list.component';
import {AnalyticsService} from '../../service/analytics.service';

@Component({
  selector: 'search',
  template: require('./search.pug'),
  directives: [PostList]
})
export class Search implements OnActivate {
    notificationsOptions = {};
    title: string;
    searchOptions: any;

    constructor(private service: PostService,
                private analyticsService: AnalyticsService) {
    }

    routerOnActivate(curr: RouteSegment, prev?: RouteSegment, currTree?: RouteTree, prevTree?: RouteTree) {
      const query = curr.getParam('query');
      this.analyticsService.track('/search/' + query);
      this.title = 'Results for \'' + query + '\'';
      this.searchOptions = {search: query};
    }
}
