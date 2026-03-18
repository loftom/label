$p = Get-CimInstance Win32_Process | Where-Object { $_.CommandLine -like '*label-system-1.0.0.jar*' } | Select-Object -First 1 -ExpandProperty ProcessId
if ($p -ne $null) { Write-Output "Stopping $p"; Stop-Process -Id $p -Force; Start-Sleep -Seconds 2 }
Start-Process -FilePath 'java' -ArgumentList '-jar','target\label-system-1.0.0.jar' -NoNewWindow -PassThru | Out-Null
Start-Sleep -Seconds 4
curl.exe -s -o preview2.png http://localhost:8080/api/images/4/preview
if (Test-Path preview2.png) { Write-Output 'preview2 saved' } else { Write-Output 'preview2 failed' }
