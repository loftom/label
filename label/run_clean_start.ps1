$p = Get-CimInstance Win32_Process | Where-Object { $_.CommandLine -like '*label-system-1.0.0.jar*' } | Select-Object -ExpandProperty ProcessId
if ($p -ne $null -and ($p -is [array] -and $p.Count -gt 0) -or ($p -isnot [array] -and $p -ne $null)) {
    Write-Output ('Stopping processes: ' + ($p -join ','))
    Stop-Process -Id $p -Force
}
Start-Process -FilePath 'java' -ArgumentList '-jar','target\\label-system-1.0.0.jar' -NoNewWindow -PassThru | Out-Null
Start-Sleep -Seconds 3
try {
    $res = Invoke-RestMethod -Uri 'http://localhost:8080/api/images' -Method Get
    Write-Output 'API OK'
} catch {
    Write-Output ('API request failed: ' + $_.Exception.Message)
}
