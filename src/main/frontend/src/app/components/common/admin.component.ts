import {OnActivate, RouteSegment, RouteTree, Router} from '@angular/router';
import {SecurityService} from '../../service/security.service';

export abstract class AdminComponent {
  routeAllowed: boolean = false;

  constructor(protected router: Router,
              protected securityService: SecurityService) {
  }

  routerOnActivate(curr: RouteSegment, prev?: RouteSegment, currTree?: RouteTree, prevTree?: RouteTree) {
    this.routeAllowed = !(curr.urlSegments.length > 0 && curr.urlSegments[0].segment == 'admin' && curr.urlSegments[0].segment != 'logout' && !this.securityService.isLogged());
    if (!this.routeAllowed) {
      this.router.navigate(['/login']);
    } else {
      this.onActivate(curr);
    }
  }

  onActivate(curr) {}
}
