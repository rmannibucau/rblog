import {Component, OnInit} from '@angular/core';
import {Router, ActivatedRoute} from '@angular/router';
import {AdminComponent} from '../../common/admin.component';
import {SecurityService} from '../../../service/security.service';
import {CategoryService} from '../../../service/category.service';
import {NotificationService} from '../../../service/notification.service';

@Component({
  selector: 'categories',
  template: require('./categories.pug')
})
export class AdminCategories extends AdminComponent implements OnInit {
  categories = [];
  notificationsOptions = {};

  constructor(private service: CategoryService,
              private notifyService: NotificationService,
              router: Router,
              route: ActivatedRoute,
              securityService: SecurityService) {
    super(router, route, securityService);
  }

  ngOnInit() {
    this.load();
  }

  private load() {
    this.service.findAll().subscribe(
        categories => this.categories = categories,
        error => this.notifyService.error('Error getting top posts', 'Can\'t retrieve top posts (HTTP ' + error.status + ').'));
  }

  deleteCategory(id) {
      this.service.removeById(id).subscribe(
        () => {
            this.notifyService.success('Deleted', 'Deleted category ' + id + '.');
            this.load();
        }, error => this.notifyService.error('Error', 'Can\'t delete category ' + id + ' (HTTP ' + error.status + ').'));
  }
}
