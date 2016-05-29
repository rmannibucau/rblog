import {Injectable, OnInit} from '@angular/core';
import {NotificationsService} from 'angular2-notifications/components';

declare var $: any;

@Injectable()
export class NotificationService implements OnInit {
  constructor(private delegate: NotificationsService) {
  }

  ngOnInit() {
    this.delegate.getChangeEmitter().onDestroy.subscribe(
      () => this.hide());
  }

  error(title, msg) {
    this.show();
    this.delegate.error(title, msg);
  }

  success(title, msg) {
    this.show();
    this.delegate.success(title, msg);
  }

  info(title, msg) {
    this.show();
    this.delegate.info(title, msg);
  }

  private show() {
    $('.simple-notification-wrapper').show();
  }

  private hide() {
    $('.simple-notification-wrapper').hide();
  }
}
