import '../vendor';

import {Component, OnInit, OnDestroy} from '@angular/core';
import {Routes, Router} from '@angular/router';
import {Location} from '@angular/common';

import {SecurityService} from './service/security.service';
import {DataProtectionLaw} from './service/cookie.service';
import {CategoryService} from './service/category.service';

import {Home} from './components/home/home.component';
import {Login} from './components/login/login.component';
import {Logout} from './components/login/login.component';
import {AdminCategory} from './components/category/admin/category.component';
import {AdminCategories} from './components/category/admin/categories.component';
import {AdminPost} from './components/post/admin/post.component';
import {AdminPosts} from './components/post/admin/posts.component';
import {AdminUser} from './components/user/admin/user.component';
import {AdminUsers} from './components/user/admin/users.component';
import {AdminProfile} from './components/profile/admin/profile.component';
import {Category} from './components/category/category.component';
import {Post} from './components/post/post.component';
import {Search} from './components/post/search.component';

declare var $: any;

@Component({
  selector: 'app',
  template: require('./app.pug')
})
@Routes([
  {path: '/', component: Home /*, useAsDefault: true */},
  {path: '/login', component: Login},
  {path: '/logout', component: Logout},
  {path: '/admin/category/new', component: AdminCategory},
  {path: '/admin/category/:id', component: AdminCategory},
  {path: '/admin/categories', component: AdminCategories},
  {path: '/admin/post/new', component: AdminPost},
  {path: '/admin/post/:id', component: AdminPost},
  {path: '/admin/posts', component: AdminPosts},
  {path: '/admin/user/new', component: AdminUser},
  {path: '/admin/user/:id', component: AdminUser},
  {path: '/admin/users', component: AdminUsers},
  {path: '/admin/profile', component: AdminProfile},
  {path: '/category/:slug', component: Category},
  {path: '/post/:slug', component: Post},
  {path: '/search', component: Search}
])
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
              private location: Location) {
    this.showDataProtectionLawMessage = !dataLowProtection.accepted;
    this.logged = this.securityService.isLogged();
    this.sub = this.securityService.lifecycleListener.subscribe(state => this.logged = state);
    categoryService.listenChanges(event => this.loadCategories());
    this.loadCategories();
	}

  ngOnInit() {
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
    this.categoryService.findAll().subscribe(cat => this.categories = cat);
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
