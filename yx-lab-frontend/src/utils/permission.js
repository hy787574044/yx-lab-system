import { getUser } from './auth'

export function getCurrentUserPermissions() {
  const user = getUser() || {}
  const permissions = Array.isArray(user.permissionCodes) ? user.permissionCodes : []
  return new Set(permissions.map((item) => String(item || '').trim()).filter(Boolean))
}

export function hasPermission(permissionCode) {
  const code = String(permissionCode || '').trim()
  if (!code) {
    return true
  }
  const permissions = getCurrentUserPermissions()
  return permissions.has('*') || permissions.has(code)
}

