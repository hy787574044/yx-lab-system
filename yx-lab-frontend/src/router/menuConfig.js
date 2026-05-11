export const labMenuGroups = [
  {
    id: 'overview',
    title: '运行总览',
    shortTitle: '总览',
    iconKey: 'DataAnalysis',
    defaultPath: '/dashboard',
    children: [
      {
        path: '/dashboard',
        title: '运行总览',
        shortTitle: '总览',
        subtitle: '统一查看样品、检测、审查与报告发布状态。',
        componentKey: 'DashboardView'
      }
    ]
  },
  {
    id: 'monitoring',
    title: '监测点位',
    shortTitle: '点位',
    iconKey: 'LocationFilled',
    defaultPath: '/monitoring-ledger',
    children: [
      {
        path: '/monitoring-ledger',
        title: '监测点位台账',
        shortTitle: '点位台账',
        subtitle: '维护监测点位基础信息，支撑采样计划与业务流转。',
        componentKey: 'MonitoringPointView'
      }
    ]
  },
  {
    id: 'sample',
    title: '样品管理',
    shortTitle: '样品',
    iconKey: 'Tickets',
    defaultPath: '/sample-login',
    children: [
      {
        path: '/sample-login',
        title: '样品登录',
        shortTitle: '样品登录',
        subtitle: '发起样品登录并承接后续检测流程。',
        componentKey: 'SamplingView',
        defaultTab: 'samples',
        defaultStatKey: 'samples:logged'
      },
      {
        path: '/sample-ledger',
        title: '样品台账',
        shortTitle: '样品台账',
        subtitle: '展示所有样品数据，并支持状态流程查询。',
        componentKey: 'SamplingView',
        defaultTab: 'samples',
        defaultStatKey: 'samples:all'
      }
    ]
  },
  {
    id: 'task',
    title: '任务管理',
    shortTitle: '任务',
    iconKey: 'List',
    defaultPath: '/task-assign',
    children: [
      {
        path: '/task-assign',
        title: '任务分配',
        shortTitle: '任务分配',
        subtitle: '对采样检测任务进行分配与派发。',
        componentKey: 'SamplingView',
        defaultTab: 'tasks',
        defaultStatKey: 'tasks:pending'
      },
      {
        path: '/task-history',
        title: '历史任务',
        shortTitle: '历史任务',
        subtitle: '展示当前登记人分配或执行过的历史任务。',
        componentKey: 'SamplingView',
        defaultTab: 'tasks',
        defaultStatKey: 'tasks:completed'
      },
      {
        path: '/task-ledger',
        title: '任务台账',
        shortTitle: '任务台账',
        subtitle: '展示所有任务记录，包括进行中与历史任务。',
        componentKey: 'SamplingView',
        defaultTab: 'tasks',
        defaultStatKey: 'tasks:all'
      }
    ]
  },
  {
    id: 'detection',
    title: '检测管理',
    shortTitle: '检测',
    iconKey: 'DataLine',
    defaultPath: '/detection-analysis',
    children: [
      {
        path: '/detection-analysis',
        title: '检测分析',
        shortTitle: '检测分析',
        subtitle: '对已分配样品进行检测分析与结果录入。',
        componentKey: 'DetectionView',
        defaultStatKey: 'all'
      },
      {
        path: '/detection-history',
        title: '历史检测',
        shortTitle: '历史检测',
        subtitle: '展示当前登记人检测分析过的历史样品数据。',
        componentKey: 'DetectionView',
        defaultStatKey: 'rejected'
      },
      {
        path: '/detection-ledger',
        title: '检测台账',
        shortTitle: '检测台账',
        subtitle: '展示全部检测分析样品数据，包括检测中与历史数据。',
        componentKey: 'DetectionView',
        defaultStatKey: 'all'
      },
      
    ]
  },
  {
    id: 'review',
    title: '审查管理',
    shortTitle: '审查',
    iconKey: 'DocumentChecked',
    defaultPath: '/review-result',
    children: [
      {
        path: '/review-result',
        title: '结果审查',
        shortTitle: '结果审查',
        subtitle: '对已检测分析的样品进行结果审查。',
        componentKey: 'ReviewView',
        defaultStatKey: 'all'
      },
      {
        path: '/review-history',
        title: '历史审查',
        shortTitle: '历史审查',
        subtitle: '展示当前登记人审查过的历史数据。',
        componentKey: 'ReviewView',
        defaultStatKey: 'approved'
      },
      {
        path: '/review-ledger',
        title: '审查台账',
        shortTitle: '审查台账',
        subtitle: '展示全部结果审查样品数据，包括审查中与历史数据。',
        componentKey: 'ReviewView',
        defaultStatKey: 'all'
      }
    ]
  },
  {
    id: 'report',
    title: '报告管理',
    shortTitle: '报告',
    iconKey: 'Document',
    defaultPath: '/report-ledger',
    children: [
      {
        path: '/report-ledger',
        title: '报告台账',
        shortTitle: '报告台账',
        subtitle: '展示所有检测报告数据，支持溯源、预览与下载等操作。',
        componentKey: 'ReportView'
      }
    ]
  },
  {
    id: 'instrument',
    title: '仪器管理',
    shortTitle: '仪器',
    iconKey: 'Cpu',
    defaultPath: '/instrument-ledger',
    children: [
      {
        path: '/instrument-ledger',
        title: '仪器设备台账',
        shortTitle: '设备台账',
        subtitle: '展示系统中仪器设备列表数据，支持增删改查操作。',
        componentKey: 'AssetView',
        defaultTab: 'inst',
        defaultStatLabel: '设备总数'
      },
      {
        path: '/instrument-maintenance',
        title: '设备维修',
        shortTitle: '设备维修',
        subtitle: '展示设备维修记录，并维护正式维修台账。',
        componentKey: 'InstrumentMaintenanceView'
      }
    ]
  },
  {
    id: 'document',
    title: '文档管理',
    shortTitle: '文档',
    iconKey: 'Files',
    defaultPath: '/document-ledger',
    children: [
      {
        path: '/document-ledger',
        title: '文档台账',
        shortTitle: '文档台账',
        subtitle: '管理文档、资料、法规制度文件、标准规范和系统帮助等内容。',
        componentKey: 'AssetView',
        defaultTab: 'doc',
        defaultStatLabel: '文档总数'
      }
    ]
  },
  {
    id: 'statistics',
    title: '分析统计',
    shortTitle: '统计',
    iconKey: 'PieChart',
    defaultPath: '/statistics-count',
    children: [
      {
        path: '/statistics-count',
        title: '化验数量统计',
        shortTitle: '数量统计',
        subtitle: '统计展示不同时间段的化验分析数量。',
        componentKey: 'StatisticsView'
      },
      {
        path: '/statistics-result',
        title: '化验结果统计',
        shortTitle: '结果统计',
        subtitle: '统计展示不同时间段的化验结果情况。',
        componentKey: 'StatisticsView'
      },
      {
        path: '/statistics-quality',
        title: '化验质量统计',
        shortTitle: '质量统计',
        subtitle: '统计展示不同时间段的化验质量情况。',
        componentKey: 'StatisticsView'
      }
    ]
  },
  {
    id: 'system',
    title: '系统管理',
    shortTitle: '系统',
    iconKey: 'Setting',
    defaultPath: '/system-users',
    children: [
      {
        path: '/system-users',
        title: '用户管理',
        shortTitle: '用户管理',
        subtitle: '对系统使用用户进行管理。',
        componentKey: 'SystemManagementView'
      },
      {
        path: '/system-orgs',
        title: '机构管理',
        shortTitle: '机构管理',
        subtitle: '对系统中用户所属机构部门进行管理。',
        componentKey: 'SystemManagementView'
      },
      {
        path: '/system-roles',
        title: '角色管理',
        shortTitle: '角色管理',
        subtitle: '对系统中用户角色及权限关系进行管理。',
        componentKey: 'SystemManagementView'
      },
      {
        path: '/system-menus',
        title: '菜单管理',
        shortTitle: '菜单管理',
        subtitle: '对系统中菜单进行管理。',
        componentKey: 'SystemManagementView'
      },
      {
        path: '/system-logs',
        title: '日志管理',
        shortTitle: '日志管理',
        subtitle: '展示系统详细的操作日志。',
        componentKey: 'SystemManagementView'
      },
      {
        path: '/system-dicts',
        title: '数据字典管理',
        shortTitle: '数据字典',
        subtitle: '对系统中数据字典进行管理。',
        componentKey: 'SystemManagementView'
      },
      {
        path: '/system-forms',
        title: '表单管理',
        shortTitle: '表单管理',
        subtitle: '对系统页面表单进行动态配置管理。',
        componentKey: 'SystemManagementView'
      },
      {
        path: '/detection-projects',
        title: '检测参数',
        shortTitle: '检测参数',
        subtitle: '维护化验参数基础台账，作为检测套餐配置来源。',
        componentKey: 'DetectionConfigView'
      },
      {
        path: '/detection-project-groups',
        title: '检测套餐',
        shortTitle: '检测套餐',
        subtitle: '将多个检测参数组合为可供样品登录选择的检测套餐。',
        componentKey: 'DetectionConfigView'
      },
      {
        path: '/detection-methods',
        title: '检测方法',
        shortTitle: '检测方法',
        subtitle: '维护化验室检测方法基础台账，支持增删改查。',
        componentKey: 'DetectionMethodView'
      },
      {
        path: '/detection-parameter-method-bindings',
        title: '参数方法绑定',
        shortTitle: '参数方法绑定',
        subtitle: '按检测参数统一配置可选检测方法，一个检测方法同一时间只能绑定到一个检测参数。',
        componentKey: 'DetectionParameterMethodBindingView'
      }
    ]
  }
]

export const legacyRedirects = [
  { path: '/monitoring', redirect: '/monitoring-ledger' },
  { path: '/samples', redirect: '/sample-login' },
  { path: '/detections', redirect: '/detection-analysis' },
  { path: '/detection-project', redirect: '/detection-projects' },
  { path: '/detection-method', redirect: '/detection-methods' },
  { path: '/reviews', redirect: '/review-result' },
  { path: '/reports', redirect: '/report-ledger' },
  { path: '/assets', redirect: '/instrument-ledger' },
  { path: '/statistics', redirect: '/statistics-count' },
  { path: '/system-context-menus', redirect: '/system-menus' }
]
