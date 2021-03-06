module.exports = {
    extends: [
        // Removes 'no-undef' lint errors for Jest global functions (`describe`, `it`, etc),
        //  add Jest-specific lint rules and Jest plugin
        // See https://github.com/jest-community/eslint-plugin-jest#recommended
        'plugin:jest/recommended',

        // Jest style rules (optional)
        'plugin:jest/style',
    ],
};
