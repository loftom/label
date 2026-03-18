$base64='iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR4nGMAAQAABQABDQottAAAAABJRU5ErkJggg=='
[IO.File]::WriteAllBytes('sample.png',[Convert]::FromBase64String($base64))
Write-Output 'Wrote sample.png'
$body = @{patientNo='P100'; name='Test User'; gender='M'}
$patient = Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/patients -Body (ConvertTo-Json $body) -ContentType 'application/json'
Write-Output ('Created patient id=' + $patient.id)
$upload = Invoke-RestMethod -Uri http://localhost:8080/api/images/upload -Method Post -Form @{ patientId = $patient.id; uploadedBy = 'tester'; file = Get-Item .\sample.png }
Write-Output ('Upload imageId=' + $upload.imageId)
$images = Invoke-RestMethod http://localhost:8080/api/images
if ($images.records.Count -gt 0) { $rec = $images.records[0]; Write-Output ('First image ID=' + $rec.id + ' patientName=' + $rec.patientName) } else { Write-Output 'No images returned' }
$imageId = $upload.imageId
Invoke-WebRequest -Uri http://localhost:8080/api/images/$imageId/preview -OutFile downloaded.png
Write-Output 'Saved preview to downloaded.png'
$p = Get-CimInstance Win32_Process | Where-Object { $_.CommandLine -like '*label-system-1.0.0.jar*' } | Select-Object -First 1 -ExpandProperty ProcessId
if ($p -ne $null) { Write-Output ('Stopping process ' + $p); Stop-Process -Id $p -Force; Start-Sleep -Seconds 2 }
Start-Process -FilePath 'java' -ArgumentList '-jar','target\label-system-1.0.0.jar' -NoNewWindow -PassThru | Out-Null
Start-Sleep -Seconds 4
Invoke-WebRequest -Uri http://localhost:8080/api/images/$imageId/preview -OutFile downloaded2.png
Write-Output 'Saved preview after restart to downloaded2.png'
