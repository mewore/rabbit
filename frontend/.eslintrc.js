module.exports = {
    root: true,
    env: {
        node: true,
    },
    extends: [
        'eslint:recommended',
        'plugin:vue/vue3-essential',
        '@vue/eslint-config-typescript/recommended',
        '@vue/eslint-config-prettier',
    ],
    parserOptions: {
        ecmaVersion: 2020,
    },
    plugins: ['prettier'],
    rules: {
        'prettier/prettier': ['error'],
        '@typescript-eslint/no-empty-function': 'off',
        'no-console': 'error',
        'no-debugger': 'error',
        'vue/require-default-prop': 'off',
        'vue/html-indent': ['error', 4],
        'vue/singleline-html-element-content-newline': 0,
        'vue/component-name-in-template-casing': ['error', 'PascalCase'],
        'comma-dangle': [
            'error',
            {
                arrays: 'always-multiline',
                objects: 'always-multiline',
                imports: 'always-multiline',
            },
        ],
        'sort-imports': 'error',
        'max-len': [
            'error',
            {
                ignorePattern: '^(import |export )',
                code: 120,
            },
        ],
        quotes: [2, 'single', { avoidEscape: true }],
        semi: 'error',
    },
    overrides: [
        {
            files: ['**/__tests__/*.{j,t}s?(x)', '**/tests/unit/**/*.spec.{j,t}s?(x)'],
            env: {
                mocha: true,
            },
            rules: {
                'sort-imports': 'error',
            },
        },
    ],
};
