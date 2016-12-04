import 'core-js/es6';
import 'core-js/es7/reflect';
import 'zone.js/dist/zone';

// safari
import './app/polyfill/date';
import 'intl';
import 'intl/locale-data/jsonp/en';

if (process.env.compileEnv === 'dev') {
  Error['stackTraceLimit'] = Infinity;
  require('zone.js/dist/long-stack-trace-zone');
}
