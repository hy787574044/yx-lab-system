const MENU_PERMISSION_MAP = {
  '/dashboard': 'dashboard:view',
  '/sample-login': 'sample:view',
  '/sample-ledger': 'sample:view',
  '/sampling-plan': 'samplingPlan:view',
  '/task-assign': 'samplingTask:view',
  '/task-history': 'samplingTask:view',
  '/task-ledger': 'samplingTask:view',
  '/detection-split': 'detection:assign',
  '/detection-analysis': 'detection:view',
  '/detection-history': 'detection:view',
  '/detection-ledger': 'detection:view',
  '/review-result': 'review:view',
  '/review-history': 'review:view',
  '/review-ledger': 'review:view',
  '/report-daily-manage': 'report:view',
  '/report-weekly-manage': 'report:view',
  '/report-half-month-manage': 'report:view',
  '/report-ledger': 'report:view',
  '/instrument-ledger': 'asset:view',
  '/instrument-maintenance': 'asset:view',
  '/document-ledger': 'asset:view',
  '/statistics-count': 'statistics:view',
  '/statistics-result': 'statistics:view',
  '/statistics-quality': 'statistics:view',
  '/system-users': 'system:view',
  '/system-orgs': 'system:view',
  '/system-roles': 'system:view',
  '/system-menus': 'system:view',
  '/system-logs': 'system:view',
  '/system-dicts': 'system:view',
  '/monitoring-ledger': 'system:view',
  '/system-forms': 'system:view',
  '/system-flow-config': 'system:view',
  '/detection-projects': 'detectionConfig:view',
  '/detection-methods': 'detectionConfig:view',
  '/detection-project-groups': 'detectionConfig:view'
}

const STAFF_ALLOWED_MENU_PATHS = new Set([
  '/dashboard',
  '/sample-login',
  '/sample-ledger',
  '/sampling-plan',
  '/task-assign',
  '/task-history',
  '/task-ledger',
  '/detection-analysis',
  '/detection-history',
  '/detection-ledger',
  '/report-daily-manage'
])

export function isStaffRole(user) {
  return String(user?.roleCode || '').trim().toUpperCase() === 'STAFF'
}

export function isMenuAllowedForRole(path, user) {
  if (!isStaffRole(user)) {
    return true
  }
  return STAFF_ALLOWED_MENU_PATHS.has(String(path || '').trim())
}

export function getMenuPermissionCode(path) {
  return MENU_PERMISSION_MAP[String(path || '').trim()] || ''
}

