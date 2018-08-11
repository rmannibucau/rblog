declare var $: any;

export class CKEditorLoader {
  loaded = false;
  hljs: any;

  constructor() {
    window['CKEDITOR_BASEPATH'] = window.location.origin + '/js/lib/ckeditor/';
  }

  loadIfNeededAndExecute(task: Function) {
    if (this.loaded) {
      task();
      return;
    }

    $.getScript('/js/lib/ckeditor/ckeditor.js', () => {
      task();
      this.loaded = true;
    });
  }

  loadHighlightJsIfNeeded(callback) {
    const self = this;
    if (!this.hljs) {
      $.ajax({
        type: "GET",
        url: '/js/lib/ckeditor/plugins/codesnippet/lib/highlight/highlight.pack.js',
        success: () => {
          $('<link/>', {
             rel: 'stylesheet',
             type: 'text/css',
             href: '/js/lib/ckeditor/plugins/codesnippet/lib/highlight/styles/idea.css'
          }).appendTo('head');

          self.hljs = window['hljs'];
          callback(self.hljs);
        },
        dataType: "script",
        cache: true // avoid to reload it each time
      });
    } else {
      callback(self.hljs);
    }
  }
}
