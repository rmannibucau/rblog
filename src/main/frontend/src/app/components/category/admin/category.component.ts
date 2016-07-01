import {Component, OnInit} from '@angular/core';
import {Router} from '@angular/router';
import {NotificationsService, SimpleNotificationsComponent} from 'angular2-notifications/components';
import {AdminComponent} from '../../common/admin.component';
import {SecurityService} from '../../../service/security.service';
import {CategoryService} from '../../../service/category.service';
import {NotificationService} from '../../../service/notification.service';

@Component({
  selector: 'category',
  template: require('./category.pug'),
  directives: [SimpleNotificationsComponent],
  providers: [NotificationsService, NotificationService]
})
export class AdminCategory extends AdminComponent implements OnInit {
    notificationsOptions = {};
    formData = {};
    categories = [];
    submitText = '';
    title = '';

    private categoryId: string;

    constructor(private service: CategoryService,
                private notifyService: NotificationService,
                router: Router,
                securityService: SecurityService) {
      super(router, securityService);
    }

    onActivate(curr) {
      this.categoryId = curr['id'];
    }

    ngOnInit() {
      this.service.findAll().subscribe(
          categories => this.categories = categories,
          error => this.notifyService.error('Error getting top posts', 'Can\'t retrieve top posts (HTTP ' + error.status + ').'));


      if (this.categoryId) {
          this.submitText = 'Update';
          this.title = 'Update category';
          this.service.findById(this.categoryId)
            .subscribe(
              category => this.formData = category,
              error => this.notifyService.error('Error getting category ' + this.categoryId, 'Can\'t retrieve the category (HTTP ' + error.status + ').'))
      } else {
          this.submitText = 'Create';
          this.title = 'New category';
      }
    }

    onSubmit() {
        if (this.formData['parentId']) { // to avoid undefined error we fake a parentId field
          this.formData['parent'] = {id: this.formData['parentId']};
        }

        this.service.save(this.formData)
          .subscribe(data => {
              if (data['parent'] && data['parent']['id']) {
                data['parentId'] = data['parent']['id'];
              }
              this.formData = data;
              this.notifyService.success('Created', 'Category created.');
          }, error => this.notifyService.error('Error', 'Error submitting the category (HTTP ' + error.status + ')'));
    }
}
