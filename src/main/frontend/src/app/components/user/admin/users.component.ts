import {Component, OnInit} from '@angular/core';
import {Router, ActivatedRoute} from '@angular/router';
import {AdminComponent} from '../../common/admin.component';
import {SecurityService} from '../../../service/security.service';
import {UserService} from '../../../service/user.service';
import {NotificationService} from '../../../service/notification.service';

@Component({
  selector: 'users',
  template: require('./users.pug')
})
export class AdminUsers extends AdminComponent implements OnInit {
    notificationsOptions = {};
    users = [];

    constructor(private service: UserService,
                private auth: SecurityService,
                private notifyService: NotificationService,
                router: Router,
                route: ActivatedRoute,
                securityService: SecurityService) {
      super(router, route, securityService);
    }

    ngOnInit() {
      this.fetchUsers();
    }

    fetchUsers() {
      this.service.findAll().subscribe(
          users => this.users = users,
          error => this.notifyService.error('Error', 'Can\'t retrieve users (HTTP ' + error.status + ').'));
    }

    deleteUser(id) {
        this.service.findById(id).subscribe(user => {
            if (this.auth.getUsername() == user.username) {
                this.notifyService.error('Error', 'You can\'t delete yourself.');
            } else {
                this.service.removeById(id).subscribe(() => {
                    this.notifyService.success('Deleted', 'Deleted user ' + id + '.');
                    this.fetchUsers();
                }, error => this.notifyService.error('Error', 'Can\'t delete user ' + id + ' (HTTP ' + error.status + ').'));
            }
        }, error => this.notifyService.error('Error', 'Can\'t find user ' + id + ' (HTTP ' + error.status + ').'));
    }
}
