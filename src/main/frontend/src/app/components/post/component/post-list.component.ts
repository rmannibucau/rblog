import {Component, Input, OnChanges, SimpleChange, AfterViewInit} from '@angular/core';
import {PostService} from '../../../service/post.service';

@Component({
  selector: 'post-list',
  template: require('./post-list.pug')()
})
export class PostList implements AfterViewInit {
  @Input() title;
  @Input() searchOptions: any;
  @Input() hasReadMore: boolean = false;
  @Input() readMoreRight: boolean = false;
  @Input() readMoreButton: boolean = true;

  page: number = 0;
  pageSize: number = 10;
  hasNext: boolean = false;
  hasPrevious: boolean = false;
  posts: Array<any>;

  constructor(private service: PostService) {
  }

  ngAfterViewInit() {
    this.searchOptions['number'] = this.pageSize; // constant for now
    this.load();
  }

  ngOnChanges(changes: {[propKey: string]: SimpleChange}) {
    if (changes && changes['searchOptions']) {
      this.load();
    }
  }

  previousPage(e) {
    e.preventDefault();
    this.page--;
    this.load();
    return false;
  }

  nextPage(e) {
    e.preventDefault();
    this.page++;
    this.load();
    return false;
  }

  private load() {
    this.updateOptions();
    this.service.search(this.searchOptions).subscribe(posts => {
      this.posts = posts;
      this.hasNext = posts.total > ((1 + this.page) * this.pageSize);
      this.hasPrevious = this.page > 0;
    });
  }

  private updateOptions() {
    this.searchOptions['offset'] = (this.page * this.pageSize);
  }
}
