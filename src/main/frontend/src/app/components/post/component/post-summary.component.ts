import {Component, Input} from '@angular/core';
import {PostService} from '../../../service/post.service';

@Component({
  selector: 'post-summary',
  template: require('./post-summary.pug'),
  styles: [
    `h2 { font-size: 22px; margin-top: 0px; margin-bottom: 5px; }`
  ]
})
export class PostSummary {
  @Input() post = {};
  @Input() hasReadMore: boolean = true;
  @Input() readMoreRight: boolean = false;
  @Input() readMoreButton: boolean = false;

  constructor(private service: PostService) {
  }

  fixDate(postDate) {
    return postDate ? new Date(postDate) : postDate;
  }
}
