export const COMMIT_TYPES = [
  'feat',
  'fix',
  'refactor',
  'test',
  'docs',
  'build',
  'ci'
];

export const COMMIT_SCOPES = [
  '用户端',
  '管理端',
  '身份',
  '用户',
  '视频',
  '媒体',
  '转码',
  '播放',
  '弹幕',
  '评论',
  '互动',
  '推荐',
  '搜索',
  '宠物',
  '智能体',
  '审核',
  '通知',
  '网关',
  '契约',
  '基础设施',
  '部署',
  '交付',
  '工作流',
  '治理'
];

export const REQUIRED_COMMIT_SECTIONS = [
  '功能明细：',
  '验证结果：',
  '未运行项：',
  '阶段状态：'
];

export const REQUIRED_PR_HEADINGS = [
  '## 变更目的',
  '## 功能明细',
  '## 接口、数据与迁移影响',
  '## 验证结果',
  '## 未运行项',
  '## 风险与回滚',
  '## Loop 证据',
  '## 阶段状态'
];

export const LOOP_TRAILERS = {
  'Streamora-Loop-Id': /^[a-z0-9][a-z0-9-]{2,80}$/,
  'Streamora-Loop-Root': /^[0-9a-f]{7,64}$/,
  'Streamora-Loop-Attempt': /^(0|[1-9][0-9]*)$/,
  'Streamora-Loop-Mode': /^(feature|ci-repair|deploy-repair|root-cause)$/
};
