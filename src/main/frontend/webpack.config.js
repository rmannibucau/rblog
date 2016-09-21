// Helper: root(), and rootDir() are defined at the bottom
var path = require('path');
var webpack = require('webpack');

// Webpack Plugins
var CommonsChunkPlugin = webpack.optimize.CommonsChunkPlugin;
var autoprefixer = require('autoprefixer');
var HtmlWebpackPlugin = require('html-webpack-plugin');
var ExtractTextPlugin = require('extract-text-webpack-plugin');
var CopyWebpackPlugin = require('copy-webpack-plugin');
var CompressionPlugin = require('compression-webpack-plugin');

// env for dev debugging
var compileEnv = process.env.APP_ENV || 'prod';
var isDev = compileEnv == 'dev';
var rootPath = '';

module.exports = function() {
  var config = {
    devtool: 'source-map',
    entry: {
     'polyfills': './src/polyfills.ts',
     'vendor': './src/vendor.ts',
     'app': './src/main.ts'
    },
    output: {
      path: root('dist'),
      publicPath: rootPath,
      filename: !isDev ? 'js/[name].[hash].js' : 'js/[name].js',
      chunkFilename: !isDev ? '[id].[hash].chunk.js' : '[id].chunk.js'
    },
    resolve: {
      unsafeCache: !isDev,
      mainFields: ["module", "main", "browser"],
      extensions: ['.ts', '.js', '.css', '.html', '.pug'],
      alias: {
        'app': 'src/app',
        'jquery': 'jquery/dist/jquery',
        'jquery-ui': 'jquery-ui/jquery-ui'
      }
    },
    module: {
      loaders: [
        {test: /\.ts$/, loader: 'ts', exclude: [/node_modules\/(?!(ng2-.+))/]},
        {test: /\.pug$/, loader: 'pug-html-loader'},
        {test: /\.html$/, loader: 'raw'},
        {test: /\.css$/, loader: 'raw'}
      ],
      noParse: [/.+zone\.js\/dist\/.+/, /.+angular2\/bundles\/.+/, /angular2-polyfills\.js/]
    }
  };

  var chunks = ['app', 'vendor', 'polyfills'];
  config.plugins = [
    new webpack.ProvidePlugin({ $: 'jquery', jQuery: 'jquery', 'window.$': 'jquery', 'window.jQuery': 'jquery' }),
    new webpack.DefinePlugin({ 'process.env': { 'compileEnv': "'" + compileEnv + "'" } }),
    new CommonsChunkPlugin({ name: chunks, minChunks: Infinity }),
    new HtmlWebpackPlugin({ template: './src/public/index.html', inject: 'body', chunksSortMode: function sort(a, b) {
        return chunks.indexOf(b.names[0]) - chunks.indexOf(a.names[0]); // reverse order
      }
    }),
    // avoid warnings: "Critical dependency: the request of a dependency is an expression"
    new webpack.ContextReplacementPlugin(/angular(\\|\/)core(\\|\/)(esm(\\|\/)src|src)(\\|\/)linker/, root('./src'))
  ];

  if (!isDev) {
    config.plugins.push(
      new webpack.NoErrorsPlugin(),
      // new webpack.optimize.DedupePlugin(),
      new webpack.optimize.UglifyJsPlugin({
        beautify: false,
        comments: false,
        mangle: {
          screw_ie8 : true,
          keep_fnames: true
        },
        compress: {
          screw_ie8: true
        }
      }),
      new CompressionPlugin({
        regExp: /\.css$|\.html$|\.js$|\.map$/,
        threshold: 2 * 1024
      })
    );
  }

  config.plugins.push(new CopyWebpackPlugin([{ from: root('src/public') }]));

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
