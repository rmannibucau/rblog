import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { PostList } from './post-list.component';
import { PostSummary } from './post-summary.component';

@NgModule({
    imports: [
      CommonModule,
      RouterModule
    ],
    declarations: [
      PostList,
      PostSummary
    ],
    exports: [
      PostList,
      PostSummary
    ]
})
export class PostModule {
}
