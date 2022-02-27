module.exports = {
    root: true,
    env: {
        node: true,
    },
    extends: ['plugin:vue/vue3-essential', 'eslint:recommended', '@vue/typescript/recommended'],
    parserOptions: {
        ecmaVersion: 2020,
    },
    rules: {
        '@typescript-eslint/no-empty-function': 'off',
        'no-console': 'error',
        'no-debugger': 'error',
        'comma-dangle': [
            'error',
            {
                arrays: 'always-multiline',
                objects: 'always-multiline',
                imports: 'always-multiline',
            },
        ],
        'sort-imports': 'error',
        indent: ['error', 4, { SwitchCase: 1 }],
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
