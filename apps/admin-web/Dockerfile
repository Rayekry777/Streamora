# syntax=docker/dockerfile:1

# Node 24 与根 packageManager 声明的 pnpm 11 配合构建管理端。
FROM node:24-alpine AS builder
WORKDIR /workspace
RUN corepack enable

# 先复制工作区清单，使用 BuildKit 缓存 pnpm 包存储。
COPY package.json pnpm-lock.yaml pnpm-workspace.yaml ./
COPY packages/ui-tokens/package.json packages/ui-tokens/package.json
COPY apps/web/package.json apps/web/package.json
COPY apps/admin-web/package.json apps/admin-web/package.json
RUN --mount=type=cache,id=streamora-pnpm,target=/pnpm/store \
    pnpm config set store-dir /pnpm/store && \
    pnpm install --frozen-lockfile --filter @streamora/admin-web...

# 管理端与用户端独立构建；敏感配置不进入 Vite 构建变量。
COPY packages/ui-tokens packages/ui-tokens
COPY apps/admin-web apps/admin-web
RUN pnpm --filter @streamora/admin-web build

# 生产镜像只包含静态文件，管理端不加载用户宠物运行时。
FROM nginx:1.28-alpine AS runtime
COPY apps/admin-web/nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=builder /workspace/apps/admin-web/dist /usr/share/nginx/html

HEALTHCHECK --interval=20s --timeout=3s --start-period=10s --retries=3 \
  CMD wget -q -O /dev/null http://127.0.0.1/healthz || exit 1

