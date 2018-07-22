import '../vendor';

import {Component, OnInit, OnDestroy} from '@angular/core';
import {Router} from '@angular/router';
import {Http} from '@angular/http';
import {Location} from '@angular/common';

import {SecurityService} from './service/security.service';
import {DataProtectionLaw} from './service/cookie.service';
import {CategoryService} from './service/category.service';

declare var $: any;

@Component({
  selector: 'app',
  template: require('./app.pug')
})
export class App implements OnInit, OnDestroy {
  private showDataProtectionLawMessage: boolean;
  private searchText: string = '';
  private categories: Array<any> = []; // todo: Category
  private logged: boolean;
  private sub;

	constructor(private securityService: SecurityService,
              private dataLowProtection: DataProtectionLaw,
              private categoryService: CategoryService,
              private router: Router,
              private location: Location,
              private http: Http) {
    this.showDataProtectionLawMessage = !dataLowProtection.accepted;
    if (!dataLowProtection.accepted) { // check we are in France, if so keep the "accept" behavior otherwise just use cookies
      http.get('//geoip.nekudo.com/api').subscribe(
          response => {
            try {
              const json = response.json();
              if (json.country && json.country.code != 'FR') {
                dataLowProtection.onAccept();
                this.showDataProtectionLawMessage = false;
              }
            } catch (e) { /*ignore*/}
          },
          error => {/*ignore, let's fallback on default case*/});
    }

    this.logged = this.securityService.isLogged();
	}

  ngOnInit() {
    this.sub = this.securityService.lifecycleListener.subscribe(state => this.logged = state);
    this.categoryService.listenChanges(event => this.loadCategories());
    this.loadCategories();
    $(document).on('click','.navbar-collapse', e => {
        if( $(e.target).is('a') ) {
            $('.navbar-ex1-collapse').collapse('hide');
        }
    });
  }

  ngOnDestroy() {
    if (this.sub != null) {
        this.sub.unsubscribe();
    }
  }

  private loadCategories() {
    this.categoryService.findAll().subscribe(
      cat => this.categories = cat);
  }

	isLogged() {
		return this.logged;
	}

  acceptCookies() {
    this.showDataProtectionLawMessage = false;
    this.dataLowProtection.onAccept();
  }

  rejectCookies() {
    this.showDataProtectionLawMessage = false;
    this.dataLowProtection.onReject();
  }

  search() {
    this.router.navigate(['/search', {query: this.searchText}]);
  }
}
