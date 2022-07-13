const HtmlWebpackPlugin = require("html-webpack-plugin")
const path = require("path")
const { CleanWebpackPlugin } = require('clean-webpack-plugin')
const CopyPlugin = require("copy-webpack-plugin")

const isProduction = process.env.NODE_ENV === 'production';
const scalaSuffix = isProduction ? "fullOpt.dest/out.js" : "fastLinkJS.dest/main.js"

module.exports = {
    name: "zioscalajs",
    entry: path.resolve(__dirname, `out/frontend/${scalaSuffix}`),
    output: {
        path: path.resolve(__dirname, "build"),
    },
    plugins: [
        new CleanWebpackPlugin({ cleanStaleWebpackAssets: false }),
        new HtmlWebpackPlugin({
            inject: "body",
            template: "frontend/ui/html/index.html"
        }),
        // Copy assets
        // new CopyPlugin({
        //     patterns: [
        //         { from: "frontend/ui/img", to: "img" },
        //     ],
        // }),
    ],
    devServer: {
        compress: true,
        port: 8000,
    },
}