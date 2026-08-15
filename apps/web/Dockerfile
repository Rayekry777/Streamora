# syntax=docker/dockerfile:1

# Node 24 与根 packageManager 声明的 pnpm 11 配合构建用户端。
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
    pnpm install --frozen-lockfile --filter @streamora/web...

# Vite 的公开变量只能放非敏感配置；API 在运行时由 Nginx 同源转发。
COPY packages/ui-tokens packages/ui-tokens
COPY packages/openapi/generated packages/openapi/generated
COPY apps/web apps/web
RUN pnpm --filter @streamora/web build

# 生产镜像只包含构建结果和 Nginx 配置，不运行 Vite 开发服务器。
FROM nginx:1.28-alpine AS runtime
COPY apps/web/nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=builder /workspace/apps/web/dist /usr/share/nginx/html

HEALTHCHECK --interval=20s --timeout=3s --start-period=10s --retries=3 \
  CMD wget -q -O /dev/null http://127.0.0.1/healthz || exit 1
