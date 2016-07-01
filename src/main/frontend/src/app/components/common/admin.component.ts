import {Router, ActivatedRoute} from '@angular/router';
import {SecurityService} from '../../service/security.service';

export abstract class AdminComponent {
  routeAllowed: boolean = false;

  private sub: any;

  constructor(protected router: Router,
              protected route: ActivatedRoute,
              protected securityService: SecurityService) {
      const path = this.route.snapshot.url.length > 0;
      this.routeAllowed = this.route.snapshot.url.length == 0 || this.route.snapshot.url[0].path === 'logout' || this.route.snapshot.url[0].path != 'admin' || this.securityService.isLogged();
      if (!this.routeAllowed) {
        this.router.navigate(['/login']);
      }
  }
}
