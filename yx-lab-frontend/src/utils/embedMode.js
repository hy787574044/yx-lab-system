const EMBEDDED_MODE_KEY = 'yx-lab-embedded-mode'

export function isEmbeddedMode() {
  return sessionStorage.getItem(EMBEDDED_MODE_KEY) === '1'
}

export function setEmbeddedMode(enabled) {
  if (enabled) {
    sessionStorage.setItem(EMBEDDED_MODE_KEY, '1')
    window.dispatchEvent(new CustomEvent('yx-lab-embedded-mode-updated', { detail: true }))
    return
  }
  sessionStorage.removeItem(EMBEDDED_MODE_KEY)
  window.dispatchEvent(new CustomEvent('yx-lab-embedded-mode-updated', { detail: false }))
}
