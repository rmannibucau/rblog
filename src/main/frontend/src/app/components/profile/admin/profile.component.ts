import {Component, OnInit} from '@angular/core';
import {Router, ActivatedRoute} from '@angular/router';
import {AdminComponent} from '../../common/admin.component';
import {SecurityService} from '../../../service/security.service';
import {NotificationService} from '../../../service/notification.service';

@Component({
  selector: 'profile',
  template: require('./profile.pug')
})
export class AdminProfile extends AdminComponent {
    notificationsOptions = {};

    constructor(private service: SecurityService,
                private notifyService: NotificationService,
                router: Router,
                route: ActivatedRoute,
                securityService: SecurityService) {
      super(router, route, securityService);
    }

    removeTokens() {
        this.service.deleteTokens().subscribe(() => {
            this.notifyService.info('Done', 'Token removed.');
        }, response => this.notifyService.error('Error', 'Can\'t delete tokens.'));
    }
}
