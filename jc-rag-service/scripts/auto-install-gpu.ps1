<#
.SYNOPSIS
  JC RAG Community 硬件自动检测 + 加速器一键安装
.DESCRIPTION
  WMI 探测物理 GPU, 按 NVIDIA 独显 > Intel Arc 集显 > CPU 优先级自动选最优加速器
  - NVIDIA (RTX 5060TI 等 Blackwell+) -> torch+cu128
  - Intel Arc 140T 等 -> torch-directml (DML backend)
  - 仅有 CPU -> 跳过
  装完后调 device_detector 验证
.NOTES
  Author  : JC
  替代   : 原 install-gpu.ps1 (写死 cu128) 保留作 legacy
  适用   : Windows 10/11 + uv 已装 + .venv 已建
#>
[CmdletBinding()]
param(
    [switch]$ForceCpu,
    [switch]$NoVerify
)

$ErrorActionPreference = "Stop"
$servicePath = "D:\Project\jc-rag\jc-rag-community\jc-rag-service"
$venvPy      = "$servicePath\.venv\Scripts\python.exe"
$logFile     = Join-Path $servicePath "auto-install-gpu.log"
$tsinghua    = "https://pypi.tuna.tsinghua.edu.cn/simple"
$pytorchIdx  = "https://download.pytorch.org/whl/cu128"

if (Test-Path $logFile) { Remove-Item $logFile -Force }
if (-not (Test-Path $venvPy)) {
    Write-Host "ERROR: venv 不存在 ($venvPy), 请先 uv sync 初始化" -ForegroundColor Red
    exit 1
}

$skipKw = 'oray','idddriver','vnc','rdp','virtual','remote','mirage','citrix','parsec'

function Test-RealGpu($name) {
    if ([string]::IsNullOrWhiteSpace($name)) { return $false }
    foreach ($kw in $skipKw) { if ($name -match $kw) { return $false } }
    return $true
}

$rawGpus = Get-CimInstance Win32_VideoController -ErrorAction SilentlyContinue
$gpus = @()
foreach ($g in $rawGpus) {
    if (Test-RealGpu $g.Name) { $gpus += $g }
}

Write-Host "=== 阶段 1: 物理 GPU 探测 ===" -ForegroundColor Cyan
if ($gpus.Count -eq 0) {
    Write-Host "  无物理 GPU" -ForegroundColor Yellow
} else {
    foreach ($g in $gpus) {
        $ram = 0
        if ($g.AdapterRAM -gt 0) { $ram = [math]::Round($g.AdapterRAM / 1GB, 1) }
        $line = "  " + $g.Name.PadRight(50) + "  " + $ram + " GB"
        Write-Host $line
    }
}

$nv  = $gpus | Where-Object { $_.Name -match 'NVIDIA|RTX|GTX|Quadro' } | Select-Object -First 1
$arc = $gpus | Where-Object { $_.Name -match 'Arc' }              | Select-Object -First 1

if     ($ForceCpu)  { $decision = 'cpu' }
elseif ($nv)        { $decision = 'nvidia_cu128' }
elseif ($arc)       { $decision = 'arc_dml' }
else                { $decision = 'cpu' }

Write-Host ""
Write-Host "决策: $decision" -ForegroundColor Green
switch ($decision) {
    'nvidia_cu128' { Write-Host ("  -> {0} -> cu128" -f $nv.Name) }
    'arc_dml'     { Write-Host ("  -> {0} -> torch-directml" -f $arc.Name) }
    'cpu'         { Write-Host "  -> 仅 CPU, 跳过 GPU 装包" }
}

Write-Host ""
Write-Host "=== 阶段 2: 装加速包 ===" -ForegroundColor Cyan
Write-Host "日志: $logFile"

switch ($decision) {
    'nvidia_cu128' {
        $pkgs = @("torch==2.7.1+cu128","torchvision==0.22.1+cu128","torchaudio==2.7.1+cu128")
        uv pip install --python $venvPy -i $tsinghua --extra-index-url $pytorchIdx --upgrade @pkgs 2>&1 | Tee-Object -FilePath $logFile -Append
        if ($LASTEXITCODE -ne 0) { Write-Host ("失败 exit=" + $LASTEXITCODE) -ForegroundColor Red; exit $LASTEXITCODE }
    }
    'arc_dml' {
        uv pip install --python $venvPy -i $tsinghua --upgrade "torch-directml" 2>&1 | Tee-Object -FilePath $logFile -Append
        if ($LASTEXITCODE -ne 0) { Write-Host ("失败 exit=" + $LASTEXITCODE) -ForegroundColor Red; exit $LASTEXITCODE }
    }
    'cpu' {
        Write-Host "跳过 GPU 装包"
    }
}

if ($NoVerify) {
    Write-Host ""
    Write-Host "=== 阶段 3 跳过 (-NoVerify) ===" -ForegroundColor Yellow
    exit 0
}

# 阶段 3: 验证 - 单引号 here-string + Replace, 避 PowerShell 5.1 解析问题
Write-Host ""
Write-Host "=== 阶段 3: 验证 device_detector ===" -ForegroundColor Cyan

$verifyPy = Join-Path $env:TEMP "jc_rag_verify_devices.py"
$pyContent = @'
import sys
sys.path.insert(0, r'__SVC_PATH__')
from app.core.device_detector import detect_all_devices, resolve_best_device
devs = detect_all_devices(None)
for d in devs:
    flag = 'OK' if d['available'] else 'X '
    print('  [' + flag + '] ' + d['id'].ljust(18) + ' rank=' + str(d['performanceRank']).rjust(3) + '  ' + d['detail'])
print('--- resolve_best_device:', resolve_best_device())
'@
$pyContent = $pyContent.Replace('__SVC_PATH__', $servicePath)
Set-Content -Path $verifyPy -Value $pyContent -Encoding utf8

& $venvPy $verifyPy 2>&1 | Tee-Object -FilePath $logFile -Append
Remove-Item $verifyPy -Force -ErrorAction SilentlyContinue

Write-Host ""
Write-Host "=== 完成 ===" -ForegroundColor Green
exit 0
