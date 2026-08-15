# 服务边界导航

服务职责和禁止项以项目根目录 `docs/architecture/SERVICE_BOUNDARIES.md` 为唯一真源，数据归属以 `docs/data/DATA_OWNERSHIP.md` 为唯一真源。

实现前必须确认：

1. 该能力属于哪个服务。
2. 哪个服务是数据唯一写入方。
3. 其他服务通过哪个 Dubbo 接口、HTTP 接口或事件获得数据。
4. 是否需要本地只读投影；投影只能由事件更新。
5. 是否正在把 `admin-service` 变成跨库写入点；如是，立即改为调用领域服务。

新增服务或转移职责必须先更新两份分类真源和 `docs/development/BACKEND_DEVELOPMENT.md`。
