$ps = Get-CimInstance Win32_Process | Where-Object { $_.CommandLine -like '*label-system-1.0.0.jar*' }
if ($ps) {
    $ids = $ps | Select-Object -ExpandProperty ProcessId
    Write-Output ('Stopping: ' + ($ids -join ','))
    Stop-Process -Id $ids -Force
} else {
    Write-Output 'No java process found'
}

Write-Output 'Running mvn package...'
& mvn -DskipTests package -U

Write-Output 'Starting jar...'
Start-Process -FilePath 'java' -ArgumentList '-jar','target\label-system-1.0.0.jar' -NoNewWindow -PassThru | Out-Null
Start-Sleep -Seconds 3
try {
    $res = Invoke-RestMethod -Uri 'http://localhost:8080/api/images' -Method Get
    Write-Output 'API OK'
} catch {
    Write-Output ('API request failed: ' + $_.Exception.Message)
}
