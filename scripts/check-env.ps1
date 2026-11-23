$ErrorActionPreference = 'Continue'
$repositoryRoot = Split-Path -Parent $PSScriptRoot
Write-Host 'Campus activity project environment check'
foreach ($command in @('git','java','javac','node','npm.cmd','mysql','docker')) {
  $found = Get-Command $command -ErrorAction SilentlyContinue
  if ($found) {
    Write-Host "$command -> $($found.Source)"
    $versionOption = if ($command -in @('java','javac')) { '-version' } else { '--version' }
    & $command $versionOption 2>&1 | Select-Object -First 3
  }
  else { Write-Host "${command}: NOT FOUND ON PATH" }
}
$wrapperPath = Join-Path $repositoryRoot 'backend/mvnw.cmd'
if (Test-Path -LiteralPath $wrapperPath) { & $wrapperPath --version }
else { Write-Host 'backend/mvnw.cmd: NOT CREATED (select Maven version before initializing backend)' }
if (Get-Command docker -ErrorAction SilentlyContinue) { docker compose version }
Write-Host 'This script is read-only and does not install tools or change databases.'
