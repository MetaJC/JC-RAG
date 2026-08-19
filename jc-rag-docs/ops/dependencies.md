# jc-rag-community 依赖约束与版本兼容性

> 社区版 Python Service (`jc-rag-community/jc-rag-service`) 的关键依赖锁定说明、升级路径、已知问题。
> 商业版 (`jc-rag-pro/`) 不在本文件范围内，其依赖策略参见 `jc-rag-pro-docs/`。

---

## 一、目录

1. [当前锁定矩阵](#二当前锁定矩阵)
2. [transformers 锁 4.45.2 的原因](#三transformers-锁-4452-的原因)
3. [torch-directml 设备路径说明](#四torch-directml-设备路径说明)
4. [已知问题](#五已知问题)
5. [升级路径](#六升级路径)
6. [回归测试](#七回归测试)
7. [变更历史](#八变更历史)

---

## 二、当前锁定矩阵

自 2026-08-19 起，依赖分为**基础依赖**（API 服务 + 文档解析 OCR，跨平台）与 **`local-model` extra**（本地模型推理，隐式拉入 torch）两层：

| 依赖 | 锁定版本 | 归属 | 备注 |
|---|---|---|---|
| `onnxruntime` | `==1.22.0` | 基础依赖 | OCR 必需，跨平台 CPU wheel |
| `rapidocr-onnxruntime` / `opencv-python` | `==1.4.4` / `==4.11.0.86` | 基础依赖 | 文档解析 OCR 链路，与 GPU 无关 |
| `transformers` | `==4.45.2` | `local-model` extra | 见第三节 |
| `torch` | 不固定 (用户自选) | extra 隐式依赖 | Arc 140T 配套 `2.4.1+cpu` + `torch-directml==0.2.5.dev240914` |
| `sentence-transformers` | `==3.3.1` | `local-model` extra | `transformers>=4.41.0,<5.0.0`，与 4.45.2 同时期官方测试矩阵 |
| `torch-directml` | `==0.2.5.dev240914` | 手动安装 (仅 Windows) | 锁定 torch 2.4.x，0.2.5.dev240914 之后无 Windows wheel |
| `onnxruntime-directml` | `==1.22.0; platform_system == 'Windows'` | `local-model` extra | Windows 专属包，平台标记隔离，Linux 上自动跳过 |

> 锁定位置：`jc-rag-community/jc-rag-service/pyproject.toml` `dependencies` 段与 `[project.optional-dependencies] local-model` 段。
> 安装方式：纯在线模型部署（`embedding.source: service`）`pip install -e .`；本地模型部署 `pip install -e .[local-model]`，torch 选型见 `pyproject.toml` 顶部安装矩阵注释。

---

## 三、transformers 锁 4.45.2 的原因

### 3.1 触发问题（2026-07-28 实测）

升级到 `transformers 4.57.6` 后，文档解析抛出：

```
ValueError: Due to a serious vulnerability issue in `torch.load`, even with
`weights_only=True`, we now require users to upgrade torch to at least v2.6
in order to use the function. This version restriction does not apply when
loading files with safetensors.
See https://nvd.nist.gov/vuln/detail/CVE-2025-32434
```

### 3.2 根因

- 错误实际抛自 `transformers/utils/import_utils.py:check_torch_load_is_safe()`
- 该函数检查 `torch >= 2.6`，否则抛错
- 社区版锁定 `torch 2.4.1`（Arc 140T 配套），与该检查**硬性冲突**
- 该检查在 `transformers 4.48+` 引入

### 3.3 为什么不能升 torch 到 2.6+

| 路径 | 后果 |
|---|---|
| 升 torch 到 2.6+ | 失去 Arc 140T GPU 加速（torch-directml 0.2.5.dev240914 仅支持 torch 2.4.x） |
| 卸 torch-directml | 同上，CPU 跑 BGE 慢 10-30 倍 |
| 升 transformers 5.0+ + 升 torch 2.6+ | 失去 Arc 140T 加速 |

### 3.4 决策

**降级 transformers 到 4.45.2**（最近一个不含 `check_torch_load_is_safe` 的稳定版）：

- ✅ 与 `sentence-transformers 3.3.1` 完全兼容（要求 `transformers>=4.41.0,<5.0.0`）
- ✅ 与 `torch 2.4.1 + torch-directml 0.2.5.dev240914` 兼容
- ✅ 真实启用 Arc 140T（依赖第四节设备路径修复）
- ⚠️ 暂时失去 transformers 4.48+ 的新特性 / 新模型支持

---

## 四、torch-directml 设备路径说明

### 4.1 修复前（已废弃代码语义）

```python
# app/embeddings/__init__.py:45-50 (修复前)
try:
    import torch_directml
    logger.info("Embedding 启用 DirectML GPU 加速")
    return "dml"   # ← bug: PyTorch 2.4.1 torch.device() 不认 "dml"
except Exception:
    pass
```

**问题**：
- PyTorch 2.4.1 `torch.device("dml")` 抛 `RuntimeError: Expected one of cpu, cuda, ... device type at start of device string: dml`
- sentence-transformers 3.3.1 内部会 `self.to(device)` → 调 `torch._C._nn._parse_to` → 触发上述异常
- 表面看起来"已启用 DirectML 加速"，实际**从未真正生效**
- 之前被 CVE 错挡住，没暴露；CVE 修复后才暴露

### 4.2 修复后（当前代码）

```python
# app/embeddings/__init__.py:45-53 (修复后)
try:
    import torch_directml
    dml_dev = torch_directml.device()      # 返回 "privateuseone:0"
    torch.zeros(1, device=dml_dev)         # 触发 backend lazy init
    logger.info("Embedding 启用 DirectML GPU 加速: %s", dml_dev)
    return str(dml_dev)                    # 真实可用 device 字符串
except Exception as ex:
    logger.warning("DirectML backend 不可用, fallback 到 CPU: %s", ex)
```

**关键点**：
- 必须用 `torch_directml.device()` 返回值（一般是 `"privateuseone:0"`）
- 必须先 `torch.zeros(1, device=dml_dev)` 触发 lazy init，否则 sentence-transformers 后续会 `ModuleNotFoundError: torch.privateuseone`
- 任一步失败 → warning + fallthrough 到 lm_studio / cpu 检查

### 4.3 验证方法

```powershell
cd D:\Project\jc-rag\jc-rag-community\jc-rag-service
.venv\Scripts\python.exe -c "
import sys; sys.path.insert(0, '.')
from app.embeddings import _resolve_device, _load_model
print(_resolve_device('auto', None))   # 应为 'privateuseone:0' 或 'cpu'
"
```

---

## 五、已知问题

### 5.1 torchaudio 2.7.1+cu128 错配（与本次修复**逻辑无关**）

```
torchaudio 2.7.1+cu128 has requirement torch==2.7.1+cu128, but you have torch 2.4.1.
```

**根因**：
- sentence-transformers 3.3.1 传递依赖的 `torchaudio` 被 pip 装成了 `2.7.1+cu128` 版（来自 cu128 仓库）
- 与 venv 实际 `torch 2.4.1+cpu` 不匹配

**影响**：
- 核心 RAG 流程**不受影响**（embedding 推理用 `transformers` 不直接调 `torchaudio`）
- 仅 `pip check` 报警

**修法**（按需执行）：
```powershell
.venv\Scripts\python.exe -m pip install --upgrade "torchaudio==2.4.1+cpu" --index-url https://download.pytorch.org/whl/cpu
```
修后**必须**重跑一次完整 RAG 流程（上传新文档 → embedding 加载 → 解析 → 检索）。

### 5.2 其他

- sentence-transformers 3.3.1 + torch 2.4.1 下，CPU 模式 `encode()` 1.27s / 2 句（BGE-large-zh-v1.5）；DirectML 模式 0.68s / 2 句，**约 2x 加速**。对 BERT 类模型不算强，但确实用到了 GPU
- 商业版 Python Service (`jc-rag-pro/`) **不受本文件影响**，其 `pyproject.toml` 独立管理依赖（`torch 2.12.1+cpu`）

---

## 六、升级路径

### 6.1 何时可以升 transformers

满足以下**全部**条件时，可考虑升级：

1. `torch-directml` 官方发布支持 `torch 2.6+` 的稳定 wheel
2. 升级到 `torch 2.6+` 后 BGE 推理在 DirectML 路径上仍工作
3. transformers 新版本中 `sentence-transformers 3.3.1` 仍能 import（建议 4.45.x → 4.46.x → 4.47.x 渐进，每次都跑回归）

### 6.2 渐进升级步骤（建议）

```bash
# 1. 改 pyproject.toml
"transformers>=4.45.2,<4.48",   # 保持 4.48 之前的安全区

# 2. 装 venv
.venv\Scripts\python.exe -m pip install "transformers>=4.45.2,<4.48"

# 3. 验证
.venv\Scripts\python.exe -c "
import sentence_transformers, transformers
print('ST:', sentence_transformers.__version__, '| TX:', transformers.__version__)
# 检查 CVE 检查是否被引入
import transformers, os
src = os.path.join(os.path.dirname(transformers.__file__), 'utils', 'import_utils.py')
content = open(src, encoding='utf-8').read()
print('CVE check present:', 'check_torch_load_is_safe' in content)
"

# 4. 跑回归（上传新文档 → 解析 → 检索）
```

### 6.3 升级后必跑回归

- [ ] import smoke test（`from sentence_transformers import SentenceTransformer`）
- [ ] 真实模型加载（`get_embedding_dimension` 或 `embed_texts`）
- [ ] 上传新文档 → parse 成功
- [ ] 检索一条问题 → 返回带 embedding 的命中
- [ ] DirectML 日志确认仍是 `privateuseone:0`（如果 Arc 140T 在用）

---

## 七、回归测试

修复后**必跑**：

```powershell
# 1. Python 服务启动日志确认
# 应见: "Embedding 启用 DirectML GPU 加速: privateuseone:0"
# 或:   "DirectML backend 不可用, fallback 到 CPU: <reason>"

# 2. 模型加载 smoke
cd D:\Project\jc-rag\jc-rag-community\jc-rag-service
.venv\Scripts\python.exe -c "
import sys; sys.path.insert(0, '.')
from app.core.config import get_settings
from app.embeddings import embed_texts
v, m = embed_texts(['测试'], get_settings(), None)
print('dim =', len(v[0]), '| device =', m.get('device'))
"

# 3. 文档解析端到端
# 在 admin 后台上传一个新 PDF, 等待 parse_status = SUCCESS
```

---

## 八、变更历史

| 日期 | 变更 | 影响范围 | 操作人 |
|---|---|---|---|
| 2026-07-28 | 降级 transformers 4.57.6 → 4.45.2，规避 CVE-2025-32434 | `pyproject.toml` + venv | JC |
| 2026-07-28 | 修复 `app/embeddings/__init__.py` `_resolve_device` dml 写死 bug，改用 `torch_directml.device()` + lazy init | `app/embeddings/__init__.py` | JC |
| 2026-07-28 | 修历史错配：torchaudio 2.7.1+cu128 → 2.4.1+cpu + torchvision 0.19.1+cpu | venv | JC |
| 2026-07-28 | 新增本文档 | `jc-rag-community/jc-rag-docs/ops/dependencies.md` | JC |
| 2026-08-19 | `onnxruntime-directml` 加 `platform_system == 'Windows'` 平台标记，修复 Linux 部署 `No matching distribution found` | `pyproject.toml` | JC |
| 2026-08-19 | 依赖分层：本地推理依赖（sentence-transformers/transformers 等）移入 `local-model` extra，纯在线模型部署不再安装 torch；代码入口补 ImportError 守卫 | `pyproject.toml` + `app/embeddings/__init__.py` + `app/api/routers/system.py` | JC |

---

> **作者**: JC
> **适用范围**: `jc-rag-community/jc-rag-service` 社区版 Python Service
> **不适用范围**: `jc-rag-pro/jc-rag-pro-service` 商业版（独立依赖策略）
