$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
Push-Location $root
try {
    & npm.cmd --prefix frontend ci --no-audit --no-fund
    if ($LASTEXITCODE -ne 0) { throw 'Frontend dependency installation failed' }
    & npm.cmd --prefix frontend run build
    if ($LASTEXITCODE -ne 0) { throw 'Frontend build failed' }
    $static = Join-Path $root 'backend/src/main/resources/static'
    New-Item -ItemType Directory -Path $static -Force | Out-Null
    Copy-Item -Path (Join-Path $root 'frontend/dist/*') -Destination $static -Recurse -Force
    Push-Location (Join-Path $root 'backend')
    try {
        & .\mvnw.cmd -B package
        if ($LASTEXITCODE -ne 0) { throw 'Backend tests or packaging failed' }
    } finally { Pop-Location }
    Write-Host 'Built backend/target/campus-activity-0.1.0.jar'
    Write-Host 'Run: java -jar backend/target/campus-activity-0.1.0.jar'
} finally { Pop-Location }
