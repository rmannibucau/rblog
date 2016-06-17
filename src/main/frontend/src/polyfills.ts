import 'core-js/es6';
import 'core-js/es7/reflect';
require('zone.js/dist/zone');

// safari
import './app/polyfill/date';
require('intl');
require('intl/locale-data/jsonp/en');

if (process.env.compileEnv === 'dev') {
  Error['stackTraceLimit'] = Infinity;
  require('zone.js/dist/long-stack-trace-zone');
}
