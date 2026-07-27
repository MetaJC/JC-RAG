# Real-time download/install monitor (no recursion, just sum child sizes)
$cacheRoot = "D:\Users\JC\AppData\Local\uv\cache"
$venvTorch = "D:\Project\jc-rag\jc-rag-community\jc-rag-service\.venv\Lib\site-packages\torch"
$venvTorchVision = "D:\Project\jc-rag\jc-rag-community\jc-rag-service\.venv\Lib\site-packages\torchvision"
$venvTorchAudio = "D:\Project\jc-rag\jc-rag-community\jc-rag-service\.venv\Lib\site-packages\torchaudio"

function Get-DirSizeBytes($path) {
    if (-not (Test-Path $path)) { return 0 }
    $items = Get-ChildItem -Path $path -Recurse -Force -ErrorAction SilentlyContinue
    return ($items | Measure-Object -Property Length -Sum).Sum
}

function Format-Bytes($bytes) {
    if ($bytes -ge 1GB) { return "{0:N2} GB" -f ($bytes / 1GB) }
    if ($bytes -ge 1MB) { return "{0:N2} MB" -f ($bytes / 1MB) }
    if ($bytes -ge 1KB) { return "{0:N2} KB" -f ($bytes / 1KB) }
    return "$bytes B"
}

$prevVenvTorch = 0
$prevTmp = 0
$startTime = Get-Date
Write-Host "=== Real-time install monitor (Ctrl+C to stop) ==="
Write-Host ("Start: {0}" -f $startTime.ToString('HH:mm:ss'))
Write-Host ""
Write-Host "Phase 1 (downloading+extracting wheels into venv):"
Write-Host "  archive-v0       = uv's installed-package cache (grows during install)"
Write-Host "  venv/site-packages/torch = final install location"
Write-Host ""
Write-Host ("{0,-10} {1,12} {2,12} {3,12} {4,12} {5,12}" -f "Time", "tmpdfDqdj", "tmp0ODhZk", "venv:torch", "venv:tv", "venv:ta")
Write-Host ("{0,-10} {1,12} {2,12} {3,12} {4,12} {5,12}" -f "----", "---------", "---------", "----------", "-------", "-------")

while ($true) {
    $now = Get-Date
    $elapsed = ($now - $startTime).TotalSeconds

    $tmp1 = Get-DirSizeBytes "$cacheRoot\.tmpdfDqdj"
    $tmp2 = Get-DirSizeBytes "$cacheRoot\.tmp0ODhZk"
    $venvT  = Get-DirSizeBytes $venvTorch
    $venvTv = Get-DirSizeBytes $venvTorchVision
    $venvTa = Get-DirSizeBytes $venvTorchAudio

    $deltaT1 = if ($tmp1 -gt $prevTmp) { Format-Bytes ($tmp1 - $prevTmp) } else { "" }
    $deltaT  = if ($venvT -gt $prevVenvTorch) { Format-Bytes ($venvT - $prevVenvTorch) } else { "" }
    $prevTmp = $tmp1
    $prevVenvTorch = $venvT

    $line = "[{0} +{1:N0}s]" -f $now.ToString('HH:mm:ss'), $elapsed
    Write-Host ("{0,-10} {1,12} {2,12} {3,12} {4,12} {5,12}" -f `
        $line, `
        (Format-Bytes $tmp1), `
        (Format-Bytes $tmp2), `
        (Format-Bytes $venvT), `
        (Format-Bytes $venvTv), `
        (Format-Bytes $venvTa)) -NoNewline

    if ($deltaT1) { Write-Host ("  tmp+{0}/s" -f $deltaT1) -NoNewline }
    if ($deltaT)  { Write-Host ("  venv+{0}/s" -f $deltaT) -NoNewline }
    Write-Host ""

    $uv = Get-Process uv -ErrorAction SilentlyContinue
    if (-not $uv) {
        Write-Host ""
        Write-Host "uv process exited."
        break
    }

    Start-Sleep -Seconds 5
}
