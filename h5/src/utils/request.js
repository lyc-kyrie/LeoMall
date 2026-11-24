import axios from 'axios'
import { showDialog, showFailToast } from 'vant';

// create an axios instance
const service = axios.create({
  baseURL: import.meta.env.VITE_BASE_API || '/leomall/wx', // api 的 base_url（.env.* 被 gitignore，兜底保证新克隆可跑）
  timeout: 5000 // request timeout
})

// request interceptor
service.interceptors.request.use(
    config => {
    if (!config.headers['X-Leomall-Token']) {
      config.headers['X-Leomall-Token'] = `${window.localStorage.getItem(
        'Authorization'
      ) || ''}`;
    }
    return config;
  },
  err => Promise.reject(err)
)

// response interceptor
service.interceptors.response.use(
  response => {
    const res = response.data

    // 标记为 silentError 的请求（如智能导购）自行负责错误提示：
    // 不弹全局 Toast/跳转，且保留完整 response 供调用方读取 errno/errmsg。
    if (res.errno !== 0 && response.config && response.config.silentError) {
      return Promise.reject(response)
    }

    if (res.errno === 501) {
        showFailToast('请登录');
        setTimeout(() => {
          window.location = '#/login/'
        }, 1500)
      return Promise.reject('error')
    } else if (res.errno === 502) {
        showFailToast('网站内部错误，请联系网站维护人员')
      return Promise.reject('error')
    } if (res.errno === 401) {
      showFailToast('参数不对');
      return Promise.reject('error')
    } if (res.errno === 402) {
      showFailToast('参数值不对');
      return Promise.reject('error')
    } else if (res.errno !== 0) {
      // 非5xx的错误属于业务错误，留给具体页面处理
      return Promise.reject(response)
    } else {
      return response
    }
  }, error => {
    // 标记为 silentError 的请求不弹全局超时弹窗，交给调用方给出准确提示
    if (error.config && error.config.silentError) {
      return Promise.reject(error)
    }
    console.log('err' + error)// for debug
    showDialog({
        title: '警告',
        message: '登录连接超时'
      });
    return Promise.reject(error)
  })

export default service
