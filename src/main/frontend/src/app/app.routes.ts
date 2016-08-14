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

export const routes = [
  {path: '', component: Home},
  {path: 'login', component: Login},
  {path: 'logout', component: Logout},
  {path: 'admin/category/new', component: AdminCategory},
  {path: 'admin/category/:id', component: AdminCategory},
  {path: 'admin/categories', component: AdminCategories},
  {path: 'admin/post/new', component: AdminPost},
  {path: 'admin/post/:id', component: AdminPost},
  {path: 'admin/posts', component: AdminPosts},
  {path: 'admin/user/new', component: AdminUser},
  {path: 'admin/user/:id', component: AdminUser},
  {path: 'admin/users', component: AdminUsers},
  {path: 'admin/profile', component: AdminProfile},
  {path: 'category/:slug', component: Category},
  {path: 'post/:slug', component: Post},
  {path: 'search', component: Search},
  {path: '**', component: Home}
];
