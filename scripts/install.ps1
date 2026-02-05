param(
    [switch]$Portable,
    [string]$Version
)

$ErrorActionPreference = 'Stop'
$repo = "tolgayilmaz86/NerveMind"

Write-Host "NerveMind Installer" -ForegroundColor Cyan
Write-Host "==================" -ForegroundColor Cyan

# 1. Fetch Release Information
$headers = @{"User-Agent" = "NerveMind-Installer" }
$release = $null

if ($Version) {
    Write-Host "Fetching version $Version..." -ForegroundColor Gray
    $releaseUrl = "https://api.github.com/repos/$repo/releases/tags/$Version"
}
else {
    Write-Host "Fetching latest release info..." -ForegroundColor Gray
    $releaseUrl = "https://api.github.com/repos/$repo/releases/latest"
}

try {
    $release = Invoke-RestMethod -Uri $releaseUrl -Headers $headers
}
catch {
    if ($Version) {
        Write-Error "Could not find release version '$Version'."
    }
    # Fallback to checking all releases if latest fails
    Write-Host "No 'latest' release found, checking all releases..." -ForegroundColor Yellow
    try {
        $allReleases = Invoke-RestMethod -Uri "https://api.github.com/repos/$repo/releases" -Headers $headers
        $release = $allReleases | Select-Object -First 1
    }
    catch {
        Write-Error "Failed to fetch any release information: $($_.Exception.Message)"
    }
}

if (-not $release) {
    Write-Error "No releases found for $repo."
}

if ($release.draft) {
    Write-Host "Note: Using a DRAFT release ($($release.tag_name))" -ForegroundColor Yellow
}

Write-Host "Found version: $($release.tag_name)" -ForegroundColor Green

# 2. Find Asset
$extension = if ($Portable) { ".zip" } else { ".msi" }
$asset = $release.assets | Where-Object { $_.name -like "*$extension" } | Select-Object -First 1

if (-not $asset) {
    $type = if ($Portable) { "portable ZIP" } else { "MSI installer" }
    Write-Error "No $type found in the release ($($release.tag_name))."
}

# 3. Setup Download Path
$tempDir = [System.IO.Path]::GetTempPath()
$downloadPath = Join-Path $tempDir $asset.name

# 4. Download
Write-Host "Downloading $($asset.name)..." -ForegroundColor Green
try {
    Invoke-WebRequest -Uri $asset.browser_download_url -OutFile $downloadPath
}
catch {
    Write-Error "Failed to download the file."
}

# 5. Handle Installation
if ($Portable) {
    # Portable Installation
    $installDir = Join-Path $HOME "NerveMind"
    Write-Host "Extracting to $installDir..." -ForegroundColor Green
    
    if (Test-Path $installDir) {
        Write-Host "Removing existing version..." -ForegroundColor Gray
        Remove-Item $installDir -Recurse -Force
    }
    
    New-Item -ItemType Directory -Path $installDir -Force | Out-Null
    Expand-Archive -Path $downloadPath -DestinationPath $installDir -Force
    
    # Cleanup download
    Remove-Item $downloadPath -Force
    
    Write-Host "Portable version ready!" -ForegroundColor Green
    Write-Host "Location: $installDir" -ForegroundColor White
    Write-Host "To run: Start-Process `"$installDir\NerveMind\NerveMind.exe`"" -ForegroundColor Yellow
}
else {
    # MSI Installation
    Write-Host "Installing MSI..." -ForegroundColor Green
    Write-Host "Please allow the installer to run if prompted." -ForegroundColor Yellow

    $proc = Start-Process -FilePath "msiexec.exe" -ArgumentList "/i `"$downloadPath`" /passive" -PassThru -Wait

    if ($proc.ExitCode -eq 0) {
        Write-Host "Installation successful!" -ForegroundColor Green
        Remove-Item $downloadPath -ErrorAction SilentlyContinue
    }
    else {
        Write-Error "Installation failed with exit code $($proc.ExitCode)."
    }
}
