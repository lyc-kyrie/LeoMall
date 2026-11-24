import Cookies from 'js-cookie'

const TokenKey = 'X-Leomall-Admin-Token'

export function getToken() {
  return Cookies.get(TokenKey)
}

export function setToken(token) {
  // P2-9：SameSite=Lax 阻止跨站请求携带管理端会话 Cookie；
  // HTTPS 下追加 Secure。开发环境 HTTP 不加 Secure，避免 Cookie 丢失。
  const attributes = { sameSite: 'Lax' }
  if (window.location.protocol === 'https:') {
    attributes.secure = true
  }
  return Cookies.set(TokenKey, token, attributes)
}

export function removeToken() {
  return Cookies.remove(TokenKey)
}
