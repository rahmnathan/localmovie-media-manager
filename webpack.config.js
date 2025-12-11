const path = require('path');
const BundleAnalyzerPlugin = require('webpack-bundle-analyzer').BundleAnalyzerPlugin;

module.exports = {
    entry: './src/main/js/App.jsx',
    cache: true,
    mode: 'production',
    output: {
        path: path.join(__dirname, 'src/main/resources/static/built'),
        filename: 'bundle.js',
        chunkFilename: '[name].bundle.js',
        publicPath: '/built/'
    },
    module: {
        rules: [
            {
                test: path.join(__dirname, '.'),
                exclude: /(node_modules)/,
                use: [{
                    loader: 'babel-loader',
                    options: {
                        presets: ["@babel/preset-env", "@babel/preset-react"]
                    }
                }]
            }
        ]
    },
    plugins: process.env.ANALYZE ? [
        new BundleAnalyzerPlugin({
            analyzerMode: 'static',
            reportFilename: 'bundle-report.html',
            openAnalyzer: false
        })
    ] : []
};