# Fetch first image, post an annotation, verify annotations and preview
try {
    $images = Invoke-RestMethod -Uri http://localhost:8080/api/images -Method Get
} catch {
    Write-Error "Failed to fetch images: $($_.Exception.Message)"
    exit 1
}
if (-not $images.records -or $images.records.Count -eq 0) {
    Write-Output "No images available to annotate"
    exit 0
}
$img = $images.records[0]
Write-Output "Using image id=$($img.id) file=$($img.fileName) patientName=$($img.patientName)"

# Build a simple labelme JSON and post as annotation
$labelme = @{version='5.0.1'; flags=@{}; shapes=@(@{label='auto-test'; points=@(@(10,10), @(50,50)); shape_type='rectangle'; flags=@{}}); imagePath=$img.fileName; imageData=$null; imageHeight=100; imageWidth=100}
$jsonBody = ($labelme | ConvertTo-Json -Depth 5)
$payload = @{ imageId = $img.id; annotator = 'scripted-agent'; jsonBody = $jsonBody } | ConvertTo-Json -Depth 5
Write-Output "Posting annotation..."
try {
    $resp = Invoke-RestMethod -Uri http://localhost:8080/api/annotations -Method Post -Body $payload -ContentType 'application/json'
    Write-Output "Posted annotation id=$($resp.id) version=$($resp.version)"
} catch {
    Write-Error "Failed to post annotation: $($_.Exception.Message)"
    exit 1
}

# Verify via GET
try {
    $anns = Invoke-RestMethod -Uri "http://localhost:8080/api/annotations?imageId=$($img.id)" -Method Get
    Write-Output "Annotations count: $($anns.Count)"
    if ($anns.Count -gt 0) {
        $latest = $anns[0]
        Write-Output "Latest annotation id=$($latest.id) annotator=$($latest.annotator) version=$($latest.version)"
        Write-Output "jsonBody: $($latest.jsonBody)"
    }
} catch {
    Write-Error "Failed to fetch annotations: $($_.Exception.Message)"
}

# Try to download preview
$previewFile = "sim_preview_$($img.id).bin"
try {
    Invoke-RestMethod -Uri "http://localhost:8080/api/images/$($img.id)/preview" -Method Get -OutFile $previewFile
    if (Test-Path $previewFile) { Write-Output "Preview saved to $previewFile" } else { Write-Output "Preview not saved" }
} catch {
    Write-Error "Preview fetch failed: $($_.Exception.Message)"
}

Write-Output 'Script finished.'
