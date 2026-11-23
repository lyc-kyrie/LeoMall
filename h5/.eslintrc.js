module.exports = {
  root: true,
  env: {
    node: true,
    es6: true
  },
  // extends: ['plugin:vue/essential', '@vue/prettier'],
  // rules: {
  //   camelcase: 'off',
  //   quotes: ['error', 'single'],
  //   indent: ['error', 2, { SwitchCase: 1 }],
  //   'no-console': process.env.NODE_ENV === 'production' ? 'error' : 'off',
  //   'no-debugger': process.env.NODE_ENV === 'production' ? 'error' : 'off'
  // },
  // 用 vue-eslint-parser 解析单文件组件，否则 eslint 会把 <template> 当 JS 解析并报错
  parser: 'vue-eslint-parser',
  parserOptions: {
    parser: 'babel-eslint',
    ecmaVersion: 2018,
    sourceType: 'module' // 允许使用 import/export 语句
  }
};
