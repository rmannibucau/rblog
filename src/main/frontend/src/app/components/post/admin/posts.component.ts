import {Component, OnInit} from '@angular/core';
import {Router} from '@angular/router';
import {NotificationsService, SimpleNotificationsComponent} from 'angular2-notifications/components';
import {AdminComponent} from '../../common/admin.component';
import {SecurityService} from '../../../service/security.service';
import {PostService} from '../../../service/post.service';
import {NotificationService} from '../../../service/notification.service';

@Component({
  selector: 'posts',
  template: require('./posts.pug'),
  directives: [SimpleNotificationsComponent],
  providers: [NotificationsService, NotificationService]
})
export class AdminPosts extends AdminComponent implements OnInit {
    notificationsOptions = {};
    page = 0;
    pageSize = 25;
    hasNext = false;
    hasPrevious = false;
    filterText: string;
    orderBy: string;
    order: string;
    posts = [];

    constructor(private postService: PostService,
                private notifyService: NotificationService,
                router: Router,
                securityService: SecurityService) {
      super(router, securityService);
    }

    ngOnInit() {
      this.fetchPosts();
    }

    doSearch() {
      this.page = 0; // reset the page in case we already changed navigating between posts to avoid to start at page N of the search result
      this.fetchPosts();
    }

    fetchPosts() {
        const request = {
            offset: this.page * this.pageSize,
            number: this.pageSize
        };
        if (this.filterText) {
            request['search'] = this.filterText;
        }
        if (this.orderBy) { // "id", "title", "slug", "type", "created", "publishDate", "author.displayName"
            request['orderBy'] = this.orderBy;
            if (this.order) {
                request['order'] = this.order;
            }
        }
        this.postService.search(request).subscribe(posts => {
            this.hasNext = posts.total > ((1 + this.page) * this.pageSize);
            this.hasPrevious = this.page > 0;
            this.posts = posts;
        }, error => this.notifyService.error('Error', 'Can\'t retrieve rows (HTTP ' + error.status + ').'));
    }

    reverseOrder() {
        this.order = this.order == 'desc' ? 'asc' : 'desc';
    }

    sortBy(type) {
        if (this.orderBy == type) {
            this.reverseOrder();
        }
        this.orderBy = type;
        this.fetchPosts();
    }

    deletePost(id) {
        this.postService.removeById(id).subscribe(() => {
            this.notifyService.success('Deleted', 'Deleted post ' + id + '.');
            this.fetchPosts();
        }, error => this.notifyService.error('Error', 'Can\'t delete post ' + id + ' (HTTP ' + error.status + ').'));
    }

    nextPage() {
        this.page++;
        this.fetchPosts();
    }

    previousPage() {
        this.page--;
        this.fetchPosts();
    }
}
