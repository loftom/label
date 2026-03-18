$json = Get-Content -Raw annotation_payload.json
Invoke-RestMethod -Uri http://localhost:8080/api/annotations -Method Post -Body $json -ContentType 'application/json'
Write-Output 'Posted'
