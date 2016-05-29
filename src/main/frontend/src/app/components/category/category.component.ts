import {Component} from '@angular/core';
import {OnActivate, RouteSegment, RouteTree} from '@angular/router';
import {NotificationsService, SimpleNotificationsComponent} from 'angular2-notifications/components';
import {CategoryService} from '../../service/category.service';
import {PostService} from '../../service/post.service';
import {PostList} from '../post/component/post-list.component';
import {AnalyticsService} from '../../service/analytics.service';
import {NotificationService} from '../../service/notification.service';

@Component({
  selector: 'category',
  template: require('./category.pug'),
  directives: [SimpleNotificationsComponent, PostList],
  providers: [NotificationsService, NotificationService]
})
export class Category {
    notificationsOptions = {};
    slug: string;
    category = {};
    searchOptions: any;

    constructor(private service: CategoryService,
                private postService: PostService,
                private notifyService: NotificationService,
                private analyticsService: AnalyticsService) {
    }

    routerOnActivate(curr: RouteSegment, prev?: RouteSegment, currTree?: RouteTree, prevTree?: RouteTree) {
      this.slug = curr.getParam('slug');
      this.analyticsService.track('/category/' + this.slug);

      this.searchOptions = {categorySlug: this.slug};
      this.fetchCategory();
    }

    fetchCategory() {
      this.service.findBySlug(this.slug)
        .subscribe(
            category => this.category = category,
            error => this.notifyService.error('Error', 'Can\'t retrieve category (HTTP ' + error.status + ').'));
    }
}
