# Streamora 前端容器部署说明

## 1. 工程事实与构建方式

- `apps/web` 与 `apps/admin-web` 是两个独立 Vue 3.5 + TypeScript + Vite 8 应用。
- 根 `package.json` 声明 pnpm 11.19，根 `pnpm-lock.yaml` 是唯一锁文件。
- 两个应用使用工作区设计令牌包，因此 Docker 构建上下文必须是仓库根目录。
- 两个应用的实际构建命令均为 `vue-tsc -b && vite build`，输出目录是 `dist`。
- 用户端 Nginx 转发 `/api/`；管理端 Nginx 只转发 `/admin-api/`。管理端不加载宠物运行时。

## 2. 生成文件

| 应用 | 构建文件 | Nginx 配置 | 本地入口 |
|---|---|---|---|
| 用户端 | `apps/web/Dockerfile` | `apps/web/nginx.conf` | `http://127.0.0.1:3000` |
| 管理端 | `apps/admin-web/Dockerfile` | `apps/admin-web/nginx.conf` | `http://127.0.0.1:3001` |

根 `.dockerignore` 负责实际 Monorepo 构建上下文的排除规则；应用目录中的 `.dockerignore` 也用于单应用检查和后续拆仓兼容。

## 3. 本地质量验证

```powershell
pnpm install --frozen-lockfile
pnpm typecheck
pnpm lint
pnpm test
pnpm build
```

分别构建镜像：

```powershell
docker build --file apps/web/Dockerfile --tag streamora/web:dev .
docker build --file apps/admin-web/Dockerfile --tag streamora/admin-web:dev .
```

验证 Nginx 配置：

```powershell
docker run --rm streamora/web:dev nginx -t
docker run --rm streamora/admin-web:dev nginx -t
```

## 4. Nginx 路由说明

- History 路由使用 `try_files $uri $uri/ /index.html`。
- `index.html` 禁止长期缓存，带哈希的 `/assets/` 文件缓存一年并标记 immutable。
- `/healthz` 返回纯文本 `ok`，用于容器健康检查。
- `proxy_pass` 不带尾部 URI，因此 `/api` 和 `/admin-api` 前缀会完整保留并交给 `gateway-service:8080`。
- 用户端为 SSE 关闭代理缓冲并使用 75 秒读取超时；后续 Agent 接口需要再验证断线恢复和心跳策略。

## 5. Compose 部署

统一使用 `platform/compose/compose.yml`，不要另建竞争入口：

```powershell
Set-Location platform/compose
docker compose --profile core up -d --build
```

只有两个 Nginx 前端向宿主机发布 Web 入口；Java 服务通过内部网络访问。

## 6. 环境变量安全

- Vite 构建变量会进入浏览器产物，只能放公开值。
- 密钥、模型 API Key、管理员令牌、Cookie 签名密钥和数据库密码不得使用 `VITE_` 变量。
- 当前 API 使用同源 Nginx 反向代理，不在构建产物中固化内网地址。

## 7. 发布、回滚和排查

- 发布前通过类型检查、Lint、单元测试、构建、`nginx -t` 与 `/healthz`。
- 使用不可变镜像标签；回滚时切换 `STREAMORA_IMAGE_TAG` 并重新部署。
- 页面刷新 404：检查 History fallback。
- API 404：确认前缀是否被保留、网关是否有对应路由。
- SSE 无增量：检查 Nginx `proxy_buffering off` 和上游超时。
- 静态资源旧版本：检查 `index.html` 未被缓存，资源文件名带内容哈希。

