import {Component} from '@angular/core';
import {Router} from '@angular/router';
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

    private sub: any;

    constructor(private service: CategoryService,
                private postService: PostService,
                private notifyService: NotificationService,
                private analyticsService: AnalyticsService,
                private router: Router) {
      this.sub = this.router
        .routerState
        .queryParams
        .subscribe(params => {
          this.slug = params['slug'];
          this.analyticsService.track('/category/' + this.slug);

          this.searchOptions = {categorySlug: this.slug};
          this.fetchCategory();
        });
    }

    ngOnDestroy() {
      this.sub.unsubscribe();
    }

    fetchCategory() {
      this.service.findBySlug(this.slug)
        .subscribe(
            category => this.category = category,
            error => this.notifyService.error('Error', 'Can\'t retrieve category (HTTP ' + error.status + ').'));
    }
}
