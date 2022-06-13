module.exports = {
    lintOnSave: false,
    devServer: {
        proxy: {
            '/socket.io': {
                target: 'http://localhost:10246',
                changeOrigin: true,
                ws:true
            }
        }
    }
}
