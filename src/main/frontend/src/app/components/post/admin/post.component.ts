import {Component, AfterViewInit} from '@angular/core';
import {Router} from '@angular/router';
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

@Component({
  selector: 'post',
  template: require('./post.pug'),
  directives: [SimpleNotificationsComponent],
  providers: [NotificationsService, NotificationService]
})
export class AdminPost extends AdminComponent implements AfterViewInit {
    notificationsOptions = {};
    dateTimePicker: any;

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
                securityService: SecurityService) {
      super(router, securityService);
      this.slugBaseUrl = window.location.href.replace(/#.*/, '') + '#/post/';
    }

    onActivate(curr) {
      this.categoryService.findAll().subscribe(
          categories => this.categories = categories,
          error => this.notifyService.error('Error', 'Can\'t retrieve categories (HTTP ' + error.status + ').'));

      const postId = curr['id'];
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
