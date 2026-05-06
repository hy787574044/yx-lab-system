const TOKEN_KEY = 'yx-lab-token'
const USER_KEY = 'yx-lab-user'

export function getToken() {
  return localStorage.getItem(TOKEN_KEY) || ''
}

export function setToken(token) {
  localStorage.setItem(TOKEN_KEY, token)
}

export function clearToken() {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(USER_KEY)
}

export function setUser(user) {
  localStorage.setItem(USER_KEY, JSON.stringify(user || {}))
}

export function getUser() {
  const value = localStorage.getItem(USER_KEY)
  return value ? JSON.parse(value) : {}
}
