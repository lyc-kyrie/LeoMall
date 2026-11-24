import dayjs from 'dayjs';
import { isNumber } from 'lodash';
export const dateFormat = (value, format = 'YYYY-MM-DD') =>
  value ? dayjs(value * 1000).format(format) : '';

export const yuan = value =>
  isNumber(value) ? `¥${(value / 100).toFixed(2)}` : value;

// Vue3 无 filters：main.js 挂到 globalProperties，模板以方法形式调用
export default {
  install(app) {
    app.config.globalProperties.yuan = yuan;
    app.config.globalProperties.dateFormat = dateFormat;
  }
};