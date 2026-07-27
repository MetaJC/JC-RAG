$servicePath = "D:\Project\jc-rag\jc-rag-community\jc-rag-service"
$logFile = Join-Path $servicePath "pip-cu128-tsinghua.log"
if (Test-Path $logFile) { Remove-Item $logFile -Force }

$tsinghua = "https://pypi.tuna.tsinghua.edu.cn/simple"
$pytorchIdx = "https://download.pytorch.org/whl/cu128"

Write-Host "=== 阶段 1: 装 torch+cu128 (清华镜像 + PyTorch 官方 cu128 源) ==="
Write-Host "日志: $logFile"
Write-Host ""

uv pip install `
    --python "$servicePath\.venv\Scripts\python.exe" `
    -i $tsinghua `
    --extra-index-url $pytorchIdx `
    --upgrade `
    "torch==2.7.1+cu128" `
    "torchvision==0.22.1+cu128" `
    "torchaudio==2.7.1+cu128" `
    2>&1 | Tee-Object -FilePath $logFile

$phase1Exit = $LASTEXITCODE
Write-Host ""
Write-Host "=== 阶段 1 完成, exit=$phase1Exit ==="
Write-Host ""

if ($phase1Exit -ne 0) {
    Write-Host "阶段 1 失败, 跳过 DirectML 阶段" -ForegroundColor Red
    exit $phase1Exit
}

Write-Host "=== 阶段 2: 装 torch-directml (Intel Arc 集显, 走清华) ==="
uv pip install `
    --python "$servicePath\.venv\Scripts\python.exe" `
    -i $tsinghua `
    --upgrade `
    "torch-directml" `
    2>&1 | Tee-Object -FilePath $logFile -Append

$phase2Exit = $LASTEXITCODE
Write-Host ""
Write-Host "=== 阶段 2 完成, exit=$phase2Exit ==="
Write-Host ""

Write-Host "=== 阶段 3: 验证 torch + CUDA + DirectML ==="
& "$servicePath\.venv\Scripts\python.exe" -c @'
import torch
print(f"torch:              {torch.__version__}")
print(f"cuda available:    {torch.cuda.is_available()}")
print(f"cuda version:      {torch.version.cuda}")
print(f"device count:      {torch.cuda.device_count()}")
if torch.cuda.is_available():
    for i in range(torch.cuda.device_count()):
        print(f"  cuda:{i} -> {torch.cuda.get_device_name(i)}")

try:
    import torch_directml
    cnt = torch_directml.device_count()
    print(f"torch-directml:    OK, device count={cnt}")
    for i in range(cnt):
        try:
            print(f"  dml:{i} -> {torch_directml.device_name(i)}")
        except Exception as e:
            print(f"  dml:{i} -> name unavailable: {e}")
except ImportError:
    print("torch-directml:    NOT INSTALLED")
except Exception as e:
    print(f"torch-directml:    IMPORT FAILED: {e}")
'@

Write-Host ""
Write-Host "=== 阶段 4: 验证 device_detector + embeddings auto device ==="
& "$servicePath\.venv\Scripts\python.exe" -c @'
import sys
sys.path.insert(0, r"D:\Project\jc-rag\jc-rag-community\jc-rag-service")
from app.core.config import get_settings
from app.core.device_detector import detect_all_devices, resolve_best_device
from app.embeddings import _resolve_device
devs = detect_all_devices(get_settings())
print("--- detect_all_devices ---")
for d in devs:
    print(f"  {d['id']:30s} rank={d['performanceRank']:3d} avail={d['available']} {d['detail']}")
print(f"--- resolve_best_device: {resolve_best_device()}")
print(f"--- embeddings._resolve_device('auto'): {_resolve_device('auto')}")
'@

Write-Host ""
Write-Host "=== 全部完成 ==="
exit 0
