import {Component, OnDestroy} from '@angular/core';
import {ActivatedRoute} from '@angular/router';
import {CategoryService} from '../../service/category.service';
import {PostService} from '../../service/post.service';
import {PostList} from '../post/component/post-list.component';
import {AnalyticsService} from '../../service/analytics.service';
import {NotificationService} from '../../service/notification.service';

@Component({
  selector: 'category',
  template: require('./category.pug')
})
export class Category implements OnDestroy {
    notificationsOptions = {};
    id: string;
    category = {};
    searchOptions: any;

    private sub: any;

    constructor(private service: CategoryService,
                private postService: PostService,
                private notifyService: NotificationService,
                private analyticsService: AnalyticsService,
                private route: ActivatedRoute) {
        this.sub = this.route.params.subscribe(params => {
          this.id = params['categoryId']
          this.analyticsService.track('/category/' + params['slug'] + '/' + this.id);
          this.searchOptions = {categoryId: this.id, light: true};
          this.fetchCategory();
        });
        
    }

    ngOnDestroy() {
      this.sub.unsubscribe();
    }

    fetchCategory() {
      this.service.findById(this.id)
        .subscribe(
            category => this.category = category,
            error => this.notifyService.error('Error', 'Can\'t retrieve category (HTTP ' + error.status + ').'));
    }
}
