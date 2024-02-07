const ESLintPlugin = require('eslint-webpack-plugin');
module.exports = {
  mode: 'production',
  devServer: {
    historyApiFallback: true,
    port: 8000,
    proxy: {
      '/api': {
           target: 'http://localhost:8000',
           router: () => 'http://localhost:8080',
           logLevel: 'debug' /*optional*/
      }
   }
  },
  resolve: {
    extensions: [".js", ".ts", ".tsx"]
  },
  plugins: [
    new ESLintPlugin({
      extensions: ['js', 'jsx', 'ts', 'tsx']
    })
  ],
  module: {
    rules: [
        {
            test: /\.tsx?$/,
            use: 'ts-loader',
            exclude: /node_modules/
        },
        {
          test: /\.css$/i,
          use: ["style-loader", "css-loader"],
        },
    ]
  }
};