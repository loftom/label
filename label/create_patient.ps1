$body = @{patientNo='P201'; name='Bob'; gender='M'}
$resp = Invoke-RestMethod -Uri http://localhost:8080/api/patients -Method Post -Body (ConvertTo-Json $body) -ContentType 'application/json'
Write-Output "created patient id=$($resp.id)"
