import {Component, OnInit} from '@angular/core';
import {Router, ActivatedRoute} from '@angular/router';
import {NotificationsService, SimpleNotificationsComponent} from 'angular2-notifications/components';
import {AdminComponent} from '../../common/admin.component';
import {SecurityService} from '../../../service/security.service';
import {UserService} from '../../../service/user.service';
import {NotificationService} from '../../../service/notification.service';

@Component({
  selector: 'user',
  template: require('./user.pug'),
  directives: [SimpleNotificationsComponent],
  providers: [NotificationsService, NotificationService]
})
export class AdminUser extends AdminComponent implements OnInit {
    notificationsOptions = {};
    formData = {};
    submitText = '';
    title = '';

    constructor(private service: UserService,
                private notifyService: NotificationService,
                router: Router,
                route: ActivatedRoute,
                securityService: SecurityService) {
      super(router, route, securityService);
    }


  ngOnInit() {
    const userId = this.route.snapshot.params['id'];
    if (userId) {
        this.submitText = 'Update';
        this.title = 'Update user';
        this.service.findById(userId).subscribe(
            user => this.formData = user,
            error => this.notifyService.error('Error', 'Can\'t retrieve user ' + userId + ' (HTTP ' + error.status + ').'));
    } else {
        this.submitText = 'Create';
        this.title = 'New user';
    }
  }

    onSubmit() {
        if (this.formData['password'] != this.formData['password2']) {
            this.notifyService.error('Error', 'Passwords don\'t match');
            return;
        }
        this.service.save(this.formData).subscribe(
          data => {
            this.notifyService.success('Created', 'User created.');
            this.formData = data;
        }, error => this.notifyService.error('Communication Error', 'Error saving the user  (HTTP ' + error.status + ')'));
    }
}
