import {Component, AfterViewInit, AfterViewChecked, OnInit, OnDestroy} from '@angular/core';
import {DomSanitizer} from '@angular/platform-browser';
import {ActivatedRoute} from '@angular/router';
import {PostService} from '../../service/post.service';
import {Twitter} from '../../service/twitter.service';
import {CKEditorLoader} from '../../service/ckeditor.service';
import {AnalyticsService} from '../../service/analytics.service';
import {NotificationService} from '../../service/notification.service';
import {HtmlMetaService} from '../../service/meta.service';

declare var $: any;

@Component({
  selector: 'post',
  template: require('./post.pug'),
  styles: [require('../../../public/js/lib/ckeditor/plugins/codesnippet/lib/highlight/styles/idea.css')]
})
export class Post implements AfterViewChecked, AfterViewInit, OnInit, OnDestroy {
    notificationsOptions = {};
    post: any;
    pageUrl: string;

    private refreshView = false;

    constructor(private service: PostService,
                private notifyService: NotificationService,
                private twitter: Twitter,
                private ckEditorLoader: CKEditorLoader,
                private analyticsService: AnalyticsService,
                private domSanitizationService : DomSanitizer,
                private route: ActivatedRoute,
                private meta: HtmlMetaService) {
    }

    ngOnInit() {
      const slug = this.route.snapshot.params['slug'];
      this.service.findBySlug(slug).subscribe(
          post => {
              this.post = post;
              this.post.content = this.domSanitizationService.bypassSecurityTrustHtml(this.post.content);
              this.refreshView = true;
              this.meta.update('title', post.title);
              this.meta.update('description', post.summary);
              this.analyticsService.track('/post/' + slug);
          }, error => this.notifyService.error('Error', 'Can\'t retrieve post (HTTP ' + error.status + ').'));
    }

    ngOnDestroy() {
      ['title', 'description'].forEach(m => this.meta.reset(m));
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
