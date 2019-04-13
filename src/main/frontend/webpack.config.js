// Helper: root(), and rootDir() are defined at the bottom
var path = require('path');
var webpack = require('webpack');

// Webpack Plugins
var autoprefixer = require('autoprefixer');
var HtmlWebpackPlugin = require('html-webpack-plugin');
var ExtractTextPlugin = require('extract-text-webpack-plugin');
var CopyWebpackPlugin = require('copy-webpack-plugin');
var TerserPlugin = require('terser-webpack-plugin');

// env for dev debugging
var compileEnv = process.env.APP_ENV || 'prod';
var isDev = compileEnv == 'dev';
var rootPath = '';

console.log('Webpack build in ' + (isDev ? 'dev' : 'prod')  + ' mode');
console.log('');
console.log('');

module.exports = function() {
  var config = {
    mode: isDev ? 'development' : 'production',
    devtool: 'source-map',
    entry: {
     app: './src/main.ts',
     polyfills: './src/polyfills.ts'
    },
    output: {
      path: root('dist'),
      publicPath: rootPath,
      filename: !isDev ? 'js/[name].[hash].js' : 'js/[name].js',
      chunkFilename: !isDev ? 'js/[name].[hash].js' : 'js/[name].js'
    },
    resolve: {
      unsafeCache: !isDev,
      mainFields: ["module", "main", "browser"],
      extensions: ['.js', '.ts', '.css', '.html', '.pug'],
      alias: {
        'app': 'src/app',
        'jquery': 'jquery/dist/jquery',
        'jquery-ui': 'jquery-ui-bundle/jquery-ui.min.js'
      }
    },
    module: {
      rules: [
        {test: /\.ts$/, use: 'ts-loader'},
        {test: /\.pug$/, use: 'pug-loader'},
        {test: /\.html$/, use: 'raw-loader'},
        {test: /\.css$/, use: 'raw-loader'}
      ]
    },
    optimization: {
      splitChunks: {
        cacheGroups: {
          vendor: {
            test: function (module) {
                var context = module.context;
                return context && context.indexOf('node_modules') >= 0;
            },
            name: 'vendor',
            chunks: 'all'
          }
        }
      }
    }
  };

  config.plugins = [
    new webpack.ProvidePlugin({ $: 'jquery', jQuery: 'jquery', 'window.$': 'jquery', 'window.jQuery': 'jquery' }),
    new webpack.DefinePlugin({ 'process.env': { 'compileEnv': "'" + compileEnv + "'" } }),
    new CopyWebpackPlugin([{ from: root('src/public') }]), // before next one otherwise index.html can miss injections
    new HtmlWebpackPlugin({ template: './src/public/index.html', inject: 'body', chunksSortMode: "manual", chunks: ['polyfills', 'vendor', 'app'] }),
    /*
    new webpack.optimize.AggressiveSplittingPlugin({
      minSize: 128,
      maxSize: 512
    })
    */
  ];

  if (!isDev) {
    config.optimization.minimize = true
    config.optimization.minimizer = [
      new TerserPlugin({
        cache: true,
        parallel: true,
        sourceMap: isDev,
        terserOptions: { // todo
          // https://github.com/webpack-contrib/terser-webpack-plugin#terseroptions
        }
      }),
    ];
    config.plugins.push(
      new webpack.NoEmitOnErrorsPlugin()
    );
  }

  return config;
}();

function root(args) {
  args = Array.prototype.slice.call(arguments, 0);
  return path.join.apply(path, [__dirname].concat(args));
}

function rootNode(args) {
  args = Array.prototype.slice.call(arguments, 0);
  return root.apply(path, ['node_modules'].concat(args));
}
