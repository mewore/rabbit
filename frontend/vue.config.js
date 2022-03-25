module.exports = {
    configureWebpack: {
        resolve: {
            fallback: {
                fs: false,
                path: false,
            },
        },
    },
    pluginOptions: {
        quasar: {
            importStrategy: 'kebab',
            rtlSupport: false,
        },
    },
    transpileDependencies: ['quasar'],
};
