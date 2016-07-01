import {Component, AfterViewInit, AfterViewChecked, OnChanges, SimpleChange, OnDestroy } from '@angular/core';
import {DomSanitizationService} from '@angular/platform-browser';
import {Router} from '@angular/router';
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
export class Post implements AfterViewChecked, AfterViewInit, OnDestroy {
    notificationsOptions = {};
    post: any;
    pageUrl: string;

    private refreshView = false;
    private sub: any;

    constructor(private service: PostService,
                private notifyService: NotificationService,
                private twitter: Twitter,
                private ckEditorLoader: CKEditorLoader,
                private analyticsService: AnalyticsService,
                private domSanitizationService : DomSanitizationService,
                private router: Router) {
      this.sub = this.router
        .routerState
        .queryParams
        .subscribe(params => {
          const slug = params['slug'];
          this.analyticsService.track('/post/' + slug);
          this.service.findBySlug(slug).subscribe(
              post => {
                  this.post = post;
                  this.post.content = this.domSanitizationService.bypassSecurityTrustHtml(this.post.content);
                  this.refreshView = true;
              }, error => this.notifyService.error('Error', 'Can\'t retrieve post (HTTP ' + error.status + ').'));
        });
    }

    ngOnDestroy() {
      this.sub.unsubscribe();
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
      return postDate ? new Date(Date.parse(postDate)) : postDate;
    }
}
