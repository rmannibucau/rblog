import {enableProdMode} from "@angular/core";
import {platformBrowserDynamic} from '@angular/platform-browser-dynamic';
import {disableDebugTools} from '@angular/platform-browser';

import {AppModule} from './app/app.module';

const ENV_PROVIDERS = [];
const isProd = process.env.compileEnv != 'dev';
if (isProd) {
  disableDebugTools();
  enableProdMode();
}

// TODO: move to ngc when webpack will get a released ngc-loader
platformBrowserDynamic()
  .bootstrapModule(AppModule);
