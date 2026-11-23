# Builds the staff app as a Windows folder with EscapeCenter.exe inside (no installer, no WiX needed).
# Usage: .\build-exe.ps1 -ServerUrl https://203.0.113.7.sslip.io
param([string]$ServerUrl = "http://localhost:8080")

$ErrorActionPreference = "Stop"
# Most Windows PCs have no JAVA_HOME and jpackage is not on PATH, so ask java where it lives.
# cmd /c, because Windows PowerShell 5.1 turns java's stderr output into an error.
$javaHome = ((cmd /c "java -XshowSettings:properties -version 2>&1" | Select-String "java.home") -split "=")[1].Trim()
$jpackage = Join-Path $javaHome "bin\jpackage.exe"
if (-not (Test-Path $jpackage)) { throw "jpackage not found in $javaHome. Install a JDK (21 or newer), not only a JRE." }

.\mvnw.cmd -q clean package -DskipTests
if ($LASTEXITCODE -ne 0) { throw "Maven build failed" }

$jar = (Get-ChildItem target\EscapeCenter-Demo-*.jar | Select-Object -First 1).Name
Copy-Item "target\$jar" target\libs\
if (Test-Path dist) { Remove-Item dist -Recurse -Force }  # fails loudly when the old app is still running

& $jpackage --type app-image --name EscapeCenter --dest dist `
    --input target\libs --main-jar $jar --main-class App.MainApp `
    --add-modules java.base,java.desktop,java.logging,java.naming,java.net.http,java.sql,java.xml,jdk.unsupported `
    --java-options "-Dserver.url=$ServerUrl"
if ($LASTEXITCODE -ne 0) { throw "jpackage failed" }

Compress-Archive dist\EscapeCenter -DestinationPath dist\EscapeCenter-Staff-Windows.zip -Force
Write-Host "Done: dist\EscapeCenter\EscapeCenter.exe and dist\EscapeCenter-Staff-Windows.zip"
