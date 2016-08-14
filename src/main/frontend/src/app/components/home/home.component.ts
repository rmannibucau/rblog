import {Component} from '@angular/core';
import {PostService} from '../../service/post.service';
import {PostSummary} from '../post/component/post-summary.component';
import {AnalyticsService} from '../../service/analytics.service';
import {NotificationService} from '../../service/notification.service';

@Component({
  selector: 'home',
  template: require('./home.pug')
})
export class Home {
  data = {};
  notificationsOptions = {};

  constructor(private postService: PostService,
              private notifyService: NotificationService,
              private analyticsService: AnalyticsService) {
    analyticsService.track('/');
    postService.top()
      .subscribe(
        top => this.data = { lasts: top.lasts, byCategories: top.byCategories, categories: Object.keys(top.byCategories) },
        error => this.notifyService.error('Error getting top posts', 'Can\'t retrieve top posts (HTTP ' + error.status + ').'));
  }
}
