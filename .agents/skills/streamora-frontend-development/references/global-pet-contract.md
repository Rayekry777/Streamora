# 全局宠物契约

`GlobalPetHost` 必须：

- 位于用户端应用 Shell 内、RouterView 外。
- 持有唯一 `PetRenderer`、位置、尺寸、显隐、身份和反应调度器。
- 未登录使用公共吉祥物；登录后原位切换个人宠物资源和状态。
- 将拖动位置限制在可视区域，并避开播放器控制栏、对话框主按钮等安全区域。
- 将位置和缩小偏好持久化；模型状态以服务端为准。
- 浏览器不支持 WebGL、资源加载失败或上下文丢失时切换静态渲染。
- 在全屏进入/退出时只移动宿主节点，不销毁渲染实例。
- 在 `prefers-reduced-motion` 下关闭空闲循环与剧烈位移，保留必要状态提示。

`PetRenderer` 最小能力：`mount`、`loadModel`、`setExpression`、`playMotion`、`resize`、`setVisible`、`dispose`。业务组件不得直接调用 Cubism SDK。

