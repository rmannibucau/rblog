export class HtmlMetaService {
    private backup: any = {};

    update(name: string, value: string) {
      if (name === 'title') { // this one is not an actual meta tag
        if (!this.backup[name]) { // save it only once (original value)
          this.backup[name] = document.title;
        }
        document.title = value;
      } else {
        let meta = document.querySelectorAll('meta[name="' + name + '"]');
        if (meta && meta.length >= 1) {
          if (!this.backup[name]) { // save it only once (original value)
            this.backup[name] = meta[0]['content'];
          }
          meta[0]['content'] = value;
        } else {
          // for now we only update description or title and it is in the main head
          // so no need to generate it on the fly
          console.log('WARNING: meta not found: ' + name);
        }
      }
    }

    reset(name: string) {
      if (this.backup[name]) {
        this.update(name, this.backup[name]);
      }
    }
}
