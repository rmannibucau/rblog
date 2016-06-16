import {enableProdMode, provide, PLATFORM_DIRECTIVES} from "@angular/core";
import {HashLocationStrategy, LocationStrategy} from '@angular/common';
import {disableDeprecatedForms, provideForms} from '@angular/forms';
import {bootstrap} from '@angular/platform-browser-dynamic';
import {ELEMENT_PROBE_PROVIDERS} from '@angular/platform-browser';
import {HTTP_PROVIDERS} from '@angular/http';
import {ROUTER_DIRECTIVES, ROUTER_PROVIDERS} from '@angular/router';

import {App} from './app/app';

import {SecurityService} from './app/service/security.service';
import {RestClient} from './app/service/rest.service';
import {DataProtectionLaw} from './app/service/cookie.service';
import {PostService} from './app/service/post.service';
import {CategoryService} from './app/service/category.service';
import {UserService} from './app/service/user.service';
import {CKEditorLoader} from './app/service/ckeditor.service';
import {Twitter} from './app/service/twitter.service';
import {AnalyticsService} from './app/service/analytics.service';

const ENV_PROVIDERS = [];
if (process.env.compileEnv === 'dev') {
  ENV_PROVIDERS.push(ELEMENT_PROBE_PROVIDERS);
} else {
  enableProdMode();
}

bootstrap(App, [
    ...HTTP_PROVIDERS,
    ...ROUTER_PROVIDERS,
    ...ENV_PROVIDERS,
    disableDeprecatedForms(),
    provideForms(),
    provide(LocationStrategy, {useClass: HashLocationStrategy}),
    //provide(APP_BASE_HREF, {useValue: '/'}),
    provide(PLATFORM_DIRECTIVES, {useValue: [ROUTER_DIRECTIVES], multi: true}),
    SecurityService,
    RestClient,
    DataProtectionLaw,
    PostService,
    CategoryService,
    UserService,
    CKEditorLoader,
    Twitter,
    AnalyticsService
])
.catch(err => console.error(err));
