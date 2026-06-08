import { setEmbeddedMode } from './embedMode'

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
  setEmbeddedMode(false)
  window.dispatchEvent(new CustomEvent('yx-lab-user-updated', { detail: {} }))
}

export function setUser(user) {
  const nextUser = user || {}
  localStorage.setItem(USER_KEY, JSON.stringify(nextUser))
  window.dispatchEvent(new CustomEvent('yx-lab-user-updated', { detail: nextUser }))
}

export function getUser() {
  const value = localStorage.getItem(USER_KEY)
  if (!value) {
    return {}
  }
  try {
    return JSON.parse(value)
  } catch {
    return {}
  }
}
