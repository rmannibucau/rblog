import {Component, AfterViewInit, AfterViewChecked, OnChanges, SimpleChange} from '@angular/core';
import {OnActivate, RouteSegment, RouteTree} from '@angular/router';
import {NotificationsService, SimpleNotificationsComponent} from 'angular2-notifications/components';
import {PostService} from '../../service/post.service';
import {Twitter} from '../../service/twitter.service';
import {CKEditorLoader} from '../../service/ckeditor.service';
import {AnalyticsService} from '../../service/analytics.service';
import {NotificationService} from '../../service/notification.service';

declare var $: any;

@Component({
  selector: 'post',
  template: require('./post.pug'),
  directives: [SimpleNotificationsComponent],
  providers: [NotificationsService, NotificationService],
  styles: [require('../../../public/js/lib/ckeditor/plugins/codesnippet/lib/highlight/styles/idea.css')]
})
export class Post implements OnActivate, AfterViewChecked, AfterViewInit {
    notificationsOptions = {};
    post: any;
    pageUrl: string;

    private refreshView = false;

    constructor(private service: PostService,
                private notifyService: NotificationService,
                private twitter: Twitter,
                private ckEditorLoader: CKEditorLoader,
                private analyticsService: AnalyticsService) {
    }

    routerOnActivate(curr: RouteSegment, prev?: RouteSegment, currTree?: RouteTree, prevTree?: RouteTree) {
      const slug = curr.getParam('slug');
      this.analyticsService.track('/post/' + slug);
      this.service.findBySlug(slug).subscribe(
          post => {
              this.post = post;
              this.refreshView = true;
          }, error => this.notifyService.error('Error', 'Can\'t retrieve post (HTTP ' + error.status + ').'));
    }

    ngAfterViewInit() {
      this.pageUrl = document.location.href;
    }

    ngAfterViewChecked() {
      if (!this.refreshView) {
        return;
      }
      const self = this;
      const codes = $('pre code').toArray();
      if (codes && codes.length > 0) {
        self.ckEditorLoader.loadHighlightJsIfNeeded(hljs => codes.forEach(block => hljs.highlightBlock(block)));
      }

      this.twitter.lazyLoad();
      this.refreshView = false;
    }

    fixDate(postDate) {
      return postDate ? new Date(postDate) : postDate;
    }
}
