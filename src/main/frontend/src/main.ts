import {enableProdMode, provide, PLATFORM_DIRECTIVES} from "@angular/core";
import {HashLocationStrategy, LocationStrategy} from '@angular/common';
import {disableDeprecatedForms, provideForms} from '@angular/forms';
import {bootstrap} from '@angular/platform-browser-dynamic';
import {enableDebugTools, disableDebugTools} from '@angular/platform-browser';
import {HTTP_PROVIDERS} from '@angular/http';
import {provideRouter, ROUTER_DIRECTIVES} from '@angular/router';

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

import {routes} from './app/app.routes';

const ENV_PROVIDERS = [];
const isProd = process.env.compileEnv != 'dev';
if (isProd) {
  disableDebugTools();
  enableProdMode();
}

bootstrap(App, [
    ...HTTP_PROVIDERS,
    ...provideRouter(routes),
    ...ENV_PROVIDERS,
    disableDeprecatedForms(),
    provideForms(),
    provide(LocationStrategy, {useClass: HashLocationStrategy}),
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
.catch(err => console.error(err))
.then(app => {
  if (!isProd) {enableDebugTools(app); }
});
