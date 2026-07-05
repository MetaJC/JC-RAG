# 知识库文档解析链路

## 目标

- 提升 PDF 文档解析吞吐，避免长时间卡在 `文档解析`
- 让进度条按真实批次推进，减少假进度
- 在后台知识库管理页展示更细的阶段日志，便于定位瓶颈

## 当前实现

- Java 端上传成功后异步提交解析任务，不阻塞上传接口返回
- Python 端按 `下载 -> 文档解析/OCR -> 文本切块 -> 向量化 -> 写入索引` 执行
- PDF 解析按动态页组切分，多进程并发执行，避免固定大页组导致长时间无反馈
- GPU OCR 场景限制 OCR 进程数，避免显存争抢；CPU 场景尽量使用多核
- 向量化和 ES 写入阶段按批次持续上报进度

## 进度与日志

- Python 每个阶段都通过内部回调把 `phase / phaseTimes / perf` 发回 Java
- Java 持久化结构化 JSON，前端据此计算每阶段百分比与日志
- 环形进度优先使用后端返回的真实 `percent`，不再只靠耗时猜测
- 进度弹窗默认自动定位到当前活跃阶段，减少“日志为空”的误判

## 关键文件

- `server/jc-rag-admin/src/main/java/com/jc/rag/service/KnowledgeBaseService.java`
- `rag-service/app/api/knowledge.py`
- `web/jc-rag-admin-web/src/views/KnowledgeBaseView.vue`
