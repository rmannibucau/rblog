import {OnDestroy} from '@angular/core';
import {Router} from '@angular/router';
import {SecurityService} from '../../service/security.service';

export abstract class AdminComponent implements OnDestroy {
  routeAllowed: boolean = false;

  private sub: any;

  constructor(protected router: Router,
              protected securityService: SecurityService) {
    this.sub = this.router
      .routerState
      .queryParams
      .subscribe(params => this.routerOnActivate(params));
  }

  ngOnDestroy() {
    this.sub.unsubscribe();
  }

  routerOnActivate(params) {
    const path = this.router.url;
    this.routeAllowed = !(path.indexOf('admin') == 0 && path.indexOf('logout') != 0 && !this.securityService.isLogged());
    if (!this.routeAllowed) {
      this.router.navigate(['/login']);
    } else {
      this.onActivate(params);
    }
  }

  onActivate(curr) {}
}
