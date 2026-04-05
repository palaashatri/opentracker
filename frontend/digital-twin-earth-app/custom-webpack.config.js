const CopyPlugin = require('copy-webpack-plugin');
const path = require('path');
const webpack = require('webpack');

module.exports = {
  plugins: [
    new CopyPlugin({
      patterns: [
        {
          from: path.join(__dirname, 'node_modules/cesium/Build/Cesium/Workers'),
          to: 'cesium/Workers'
        },
        {
          from: path.join(__dirname, 'node_modules/cesium/Build/Cesium/ThirdParty'),
          to: 'cesium/ThirdParty'
        },
        {
          from: path.join(__dirname, 'node_modules/cesium/Build/Cesium/Assets'),
          to: 'cesium/Assets'
        },
        {
          from: path.join(__dirname, 'node_modules/cesium/Build/Cesium/Widgets'),
          to: 'cesium/Widgets'
        }
      ]
    }),
    new webpack.DefinePlugin({
      CESIUM_BASE_URL: JSON.stringify('/cesium')
    })
  ]
};
