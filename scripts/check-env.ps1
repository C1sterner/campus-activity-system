$ErrorActionPreference = 'Continue'
Write-Host 'Campus activity project environment check'
foreach ($command in @('git','java','javac','node','npm','mysql','docker')) {
  $found = Get-Command $command -ErrorAction SilentlyContinue
  if ($found) { & $command --version 2>&1 | Select-Object -First 1 }
  else { Write-Host "${command}: NOT FOUND" }
}
if (Test-Path '.\mvnw.cmd') { .\mvnw.cmd --version }
else { Write-Host 'mvnw.cmd: NOT FOUND (create backend wrapper after selecting Maven version)' }
Write-Host 'This script is read-only and does not install tools or change databases.'
