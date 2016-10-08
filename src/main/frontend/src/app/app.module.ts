import {NgModule} from '@angular/core';
import {BrowserModule } from '@angular/platform-browser';
import {FormsModule} from '@angular/forms';
import {RouterModule} from '@angular/router';
import {HttpModule} from '@angular/http';
import {SimpleNotificationsModule} from 'angular2-notifications/components';

import {App}   from './app';
import {PostModule}   from './components/post/component/post.module';

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

import {NotificationService} from './service/notification.service';
import {SecurityService} from './service/security.service';
import {RestClient} from './service/rest.service';
import {DataProtectionLaw} from './service/cookie.service';
import {PostService} from './service/post.service';
import {CategoryService} from './service/category.service';
import {UserService} from './service/user.service';
import {CKEditorLoader} from './service/ckeditor.service';
import {Twitter} from './service/twitter.service';
import {AnalyticsService} from './service/analytics.service';
import {HtmlMetaService} from './service/meta.service';
import {BitlyService} from './service/bitly.service';
import {BackupService} from './service/backup.service';

import {routes} from './app.routes';

@NgModule({
    bootstrap: [ App ],
    declarations: [
      // app component
      App,
      Home,
      Login,
      Logout,
      AdminCategory,
      AdminCategories,
      AdminPost,
      AdminPosts,
      AdminUser,
      AdminUsers,
      AdminProfile,
      Category,
      Post,
      Search
    ],
    providers: [
      // app services
      NotificationService,
      SecurityService,
      RestClient,
      DataProtectionLaw,
      PostService,
      CategoryService,
      UserService,
      CKEditorLoader,
      Twitter,
      AnalyticsService,
      HtmlMetaService,
      BitlyService,
      BackupService
    ],
    imports: [
      // app modules
      PostModule,
      // angular2-notifications
      SimpleNotificationsModule,
      // angular built-in modules
      BrowserModule,
      FormsModule,
      HttpModule,
      RouterModule.forRoot(routes, { useHash: true })
    ]
})
export class AppModule {
}
