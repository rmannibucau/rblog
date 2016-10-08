import {Component, OnInit} from '@angular/core';
import {Router, ActivatedRoute} from '@angular/router';
import {AdminComponent} from '../../common/admin.component';
import {SecurityService} from '../../../service/security.service';
import {NotificationService} from '../../../service/notification.service';
import {BackupService} from '../../../service/backup.service';

@Component({
  selector: 'profile',
  template: require('./profile.pug')
})
export class AdminProfile extends AdminComponent {
    notificationsOptions = {};

    constructor(private service: SecurityService,
                private notifyService: NotificationService,
                private backupService: BackupService,
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

    backup() {
        this.backupService.backup().subscribe(
            zip => { // run the actual downloading - designed for chrome ATM
                const blob = new Blob([zip.arrayBuffer()], {type: 'application/zip'}); // zip.blob() doesn't work
                const tempUrl = URL.createObjectURL(blob);
                try {
                    const elt = document.createElement('a');
                    document.body.appendChild(elt);
                    elt['href'] = tempUrl;
                    elt['download'] = 'backup-' + new Date().getTime() + '.zip';
                    elt.click();
                    document.body.removeChild(elt);
                } finally {
                    URL.revokeObjectURL(tempUrl);
                }
            },
            response => this.notifyService.error('Error', 'Can\'t download the backup.'));
    }
}
