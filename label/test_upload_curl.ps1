$resp = curl.exe -s -H "Content-Type: application/json" -d '{"patientNo":"P103","name":"Test User","gender":"M"}' http://localhost:8080/api/patients
Write-Output "patient response: $resp"
$id = (ConvertFrom-Json $resp).id
Write-Output "patient id=$id"
$up = curl.exe -s -F "file=@sample.png" -F "patientId=$id" -F "uploadedBy=tester" http://localhost:8080/api/images/upload
Write-Output "upload response: $up"
$imageId = (ConvertFrom-Json $up).imageId
Write-Output "imageId=$imageId"
curl.exe -s http://localhost:8080/api/images | Write-Output
curl.exe -s -o downloaded.png http://localhost:8080/api/images/$imageId/preview
Write-Output "Saved preview to downloaded.png"
