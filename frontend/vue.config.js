const { defineConfig } = require('@vue/cli-service')

module.exports = defineConfig({
  transpileDependencies: true,
  devServer: {
    host: '0.0.0.0',
    port: 3000,
    hot: true,
    liveReload: true,
    // Allow connections from any host (needed for Docker)
    allowedHosts: 'all',
    // Enable client overlay for errors
    client: {
      overlay: {
        errors: true,
        warnings: false,
      },
      webSocketURL: 'ws://localhost:3000/ws',
    },
    // Use watchFiles instead of watchOptions for newer webpack-dev-server
    watchFiles: {
      paths: ['src/**/*', 'public/**/*'],
      options: {
        usePolling: true,
        interval: 1000,
      },
    },
  },
  // Configure webpack for better file watching
  configureWebpack: {
    watchOptions: {
      poll: 1000,
      aggregateTimeout: 300,
      ignored: /node_modules/,
    },
  },
})