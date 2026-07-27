<#
.SYNOPSIS
  JC RAG Community venv 一键重建 (uv sync --refresh + 自动装 torch)
.DESCRIPTION
  默认: uv sync --refresh 重装非 torch 依赖, 然后调 auto-install-gpu 装硬件加速
  --Full : 先 rm -rf .venv 完全重建, 再 uv sync, 再装 torch
.NOTES
  Author : JC
  适用   : venv 损坏 / 包版本冲突 / 升级 Python 小版本时
#>
[CmdletBinding()]
param(
    [switch]$Full,
    [switch]$ForceCpu,
    [switch]$NoGpu
)

$ErrorActionPreference = "Stop"
$scriptDir   = Split-Path -Parent $MyInvocation.MyCommand.Path
$servicePath = Split-Path -Parent $scriptDir
$venvDir     = "$servicePath\.venv"
$logFile     = Join-Path $servicePath "rebuild-venv.log"
$tsinghua    = "https://pypi.tuna.tsinghua.edu.cn/simple"

if (Test-Path $logFile) { Remove-Item $logFile -Force }

Write-Host "=== 阶段 1: 重建 venv ===" -ForegroundColor Cyan
Write-Host ("服务路径: " + $servicePath)
Write-Host ("日志: " + $logFile)

if ($Full) {
    Write-Host ""
    Write-Host ("  [Full] 删除旧 venv: " + $venvDir) -ForegroundColor Yellow
    if (Test-Path $venvDir) {
        Remove-Item -Recurse -Force $venvDir
        Write-Host "  [Full] 已删除"
    }
    Write-Host "  uv venv (Python 3.10)"
    uv venv "$venvDir" --python 3.10 2>&1 | Tee-Object -FilePath $logFile -Append
    if ($LASTEXITCODE -ne 0) { Write-Host "uv venv 失败" -ForegroundColor Red; exit $LASTEXITCODE }
}

Write-Host ""
Write-Host "  uv sync --refresh (重装非 torch 依赖)"
uv sync --refresh --directory $servicePath -i $tsinghua 2>&1 | Tee-Object -FilePath $logFile -Append
if ($LASTEXITCODE -ne 0) { Write-Host "uv sync 失败" -ForegroundColor Red; exit $LASTEXITCODE }

if ($NoGpu) {
    Write-Host ""
    Write-Host "=== 跳过 GPU 装包 (-NoGpu) ===" -ForegroundColor Yellow
    exit 0
}

Write-Host ""
Write-Host "=== 阶段 2: 自动装 torch 加速包 ===" -ForegroundColor Cyan
$autoScript = Join-Path $scriptDir "auto-install-gpu.ps1"
if (-not (Test-Path $autoScript)) {
    Write-Host ("ERROR: 找不到 " + $autoScript) -ForegroundColor Red
    exit 1
}

$autoArgs = @{}
if ($ForceCpu) { $autoArgs['ForceCpu'] = $true }
& $autoScript @autoArgs 2>&1 | Tee-Object -FilePath $logFile -Append
if ($LASTEXITCODE -ne 0) { Write-Host "auto-install-gpu 失败" -ForegroundColor Red; exit $LASTEXITCODE }

Write-Host ""
Write-Host "=== venv 重建完成 ===" -ForegroundColor Green
exit 0
