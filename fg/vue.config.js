module.exports = {
    devServer: {
        proxy: {
            '/api': {
                target: 'http://localhost:80',
                changeOrigin: true,
                pathRewrite: {
                    '^/api': ''
                }
            }
        }
    }
}