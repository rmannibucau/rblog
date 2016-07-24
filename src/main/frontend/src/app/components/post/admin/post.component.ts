import {Component, OnInit, AfterViewInit} from '@angular/core';
import {Router, ActivatedRoute} from '@angular/router';
import {NotificationsService, SimpleNotificationsComponent} from 'angular2-notifications/components';
import {AdminComponent} from '../../common/admin.component';
import {SecurityService} from '../../../service/security.service';
import {PostService} from '../../../service/post.service';
import {CategoryService} from '../../../service/category.service';
import {UserService} from '../../../service/user.service';
import {CKEditorLoader} from '../../../service/ckeditor.service';
import {NotificationService} from '../../../service/notification.service';

declare var $: any;
declare var CKEDITOR: any;

const INPUT_DATE_FORMAT = 'MM/DD/YYYY HH:MM A';

const TWEETER_LINK_PREFIXES = ['http://', 'https://'];
const TWEETER_MAX_LEN = 140;
const TWEEK_LINK_LEN = 23;

@Component({
  selector: 'post',
  template: require('./post.pug'),
  directives: [SimpleNotificationsComponent],
  providers: [NotificationsService, NotificationService]
})
export class AdminPost extends AdminComponent implements OnInit, AfterViewInit {
    notificationsOptions = {};
    dateTimePicker: any;

    maxNotificationsChars = TWEETER_MAX_LEN;
    formData = {type: 'POST', categories: [], notification: {}};
    submitText = '';
    title = '';
    slugBaseUrl = '';
    categories = [];
    users = [];
    viewInit = false;

    constructor(private postService: PostService,
                private userService: UserService,
                private categoryService: CategoryService,
                private ckEditorLoader: CKEditorLoader,
                private notifyService: NotificationService,
                router: Router,
                route: ActivatedRoute,
                securityService: SecurityService) {
      super(router, route, securityService);
      this.slugBaseUrl = window.location.href.replace(/#.*/, '') + '#/post/';
    }

    ngOnInit() {
      this.categoryService.findAll().subscribe(
          categories => this.categories = categories,
          error => this.notifyService.error('Error', 'Can\'t retrieve categories (HTTP ' + error.status + ').'));

      const postId = this.route.snapshot.params['id'];
      if (postId) {
          this.fetchUsers(false);

          this.submitText = 'Update';
          this.title = 'Update post';
          this.postService.findById(postId).map(p => this.postLoadPost(p)).subscribe(
            post => {
                this.formData = post;
                if (this.viewInit) {
                    this.initEditor();
                }
            },
            error => this.notifyService.error('Error', 'Can\'t retrieve category ' + postId + ' (' + error.text() + ').'));
      } else {
          this.fetchUsers(true);

          let now = new Date().toISOString();
          now = now.substring(0, now.indexOf('.') - 3 /*seconds*/);

          this.submitText = 'Create';
          this.title = 'New post';
          this.formData['content'] = 'Write what you want...';
          this.formData['published'] = now;
      }
    }

    ngAfterViewInit() {
      if (this.formData['content']) {
        this.initEditor();
      }
      this.viewInit = true;
    }

    fetchUsers(selectOne) {
        this.userService.findAll().subscribe(
            users => {
                this.users = users;
                if (selectOne) {
                    this.formData['author'] = users[0];
                }
            }, error => this.notifyService.error('Error', 'Can\'t retrieve users (HTTP ' + error.status + ').'));
    }

    onCategorySelect(evt) {
      const id = evt.target.value;
      const originalSize = this.formData.categories.length;
      this.formData.categories = this.formData.categories.filter(c => c.id == id);
      if (this.formData.categories.length == originalSize) { // then we need to add it
        this.categories.filter(c => c.id == id).forEach(c => this.formData.categories.push(c));
      }
    }

    isCategorySelected(cat) {
      return this.formData.categories.map(c => c.id).indexOf(cat.id) >= 0;
    }

    onSubmit() {
        const copy = $.extend(true, {}, this.formData);
        if (copy.published && copy.published.length <= 16) { // miss seconds and more important timezone
          copy.published = copy.published + ':00Z'; // UTC by default
        }

        this.postService.save(copy).map(p => this.postLoadPost(p)).subscribe(result => {
            this.router.navigate(['/admin/posts']);
        }, error => this.notifyService.error('Error', 'Can\'t save post (HTTP ' + error.status + ').'));
    }

    maxTweeterMessageLength(message) { // see com.github.rmannibucau.rblog.social.TwitterService#messageLength
      if (!message) {
        return TWEETER_MAX_LEN;
      }
      return TWEETER_MAX_LEN - TWEETER_LINK_PREFIXES.map(prefix => {
        let diff = 0;
        let startIdx = 0;
        do {
            const linkIdx = message.indexOf(prefix, startIdx);
            if (linkIdx >= 0) {
                const realEnd = Math.min.apply(null, [message.indexOf(' ', linkIdx), message.indexOf('\n', linkIdx), message.length].filter(i => i >= startIdx + prefix.length));
                const linkLen = realEnd - linkIdx;
                diff += TWEEK_LINK_LEN - linkLen;
                startIdx = realEnd;
                continue;
            }
            break;
        } while (startIdx > 0 && startIdx < message.length);
        return diff;
      }).reduce((a, b) => a + b, 0);
    }

    remainingCharacters(message) {
      return this.maxTweeterMessageLength(message) - (message ? message.length : 0);
    }

    private postLoadPost(post) {
      post.notification = post.notification || {};
      if (post.published && post.published.length > 16) { // remove > second part of the string
        post.published = post.published.substring(0, 16);
      }
      return post;
    }

    private initEditor() {
      const content = this.formData['content'] || 'Loading...';
      this.ckEditorLoader.loadIfNeededAndExecute(() => {
        if (CKEDITOR.env.ie && CKEDITOR.env.version < 9) {
            CKEDITOR.tools.enableHtml5Elements(document);
        }
        CKEDITOR.document.getById('postEditor').setHtml(content);
        const editor = CKEDITOR.replace('postEditor', {
             height: 400,
             width: 'auto',
             language: 'es'
         });
        editor.on("change", () => this.formData['content'] = editor.getData());
      });
    }
}
