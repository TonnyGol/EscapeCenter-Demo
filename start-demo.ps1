# Starts the whole stack on this computer for the local demo: .\start-demo.ps1
# First run creates .env with random passwords, so nothing secret is committed and nothing needs editing.
$ErrorActionPreference = "Stop"

$EnvFile = ".env"
$HealthUrl = "http://localhost:8080/actuator/health"
$WaitSeconds = 180

function New-RandomPassword {
    $bytes = New-Object byte[] 18
    [System.Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($bytes)
    return [Convert]::ToBase64String($bytes).Replace("+", "a").Replace("/", "b").Replace("=", "c")
}

if (-not (Test-Path $EnvFile)) {
    @"
MYSQL_ROOT_PASSWORD=$(New-RandomPassword)
MYSQL_DATABASE=escapecenter
MYSQL_USER=escape
MYSQL_PASSWORD=$(New-RandomPassword)
APP_ADMIN_USER=admin
APP_ADMIN_PASSWORD=$(New-RandomPassword)
RABBITMQ_USER=escape
RABBITMQ_PASSWORD=$(New-RandomPassword)
"@ | Set-Content $EnvFile -Encoding ascii
    Write-Host "Created $EnvFile with random passwords."
}

# Windows PowerShell 5.1 turns docker's progress output (stderr) into errors, so check the exit code instead.
$ErrorActionPreference = "Continue"
docker compose up -d --build
$dockerExitCode = $LASTEXITCODE
$ErrorActionPreference = "Stop"
if ($dockerExitCode -ne 0) { throw "docker compose failed. Is Docker Desktop running?" }

Write-Host "Waiting for the API (the first start takes a few minutes)..."
$deadline = (Get-Date).AddSeconds($WaitSeconds)
do {
    try { $ready = (Invoke-RestMethod $HealthUrl -TimeoutSec 3).status -eq "UP" } catch { $ready = $false }
    if (-not $ready -and (docker compose ps api --status exited -q)) {
        throw "The API stopped. Run: docker compose logs api"
    }
    if (-not $ready) { Start-Sleep 3 }
} until ($ready -or (Get-Date) -gt $deadline)
if (-not $ready) { throw "The API did not come up in $WaitSeconds seconds. Run: docker compose logs api" }

function Get-EnvValue($name) {
    (Select-String -Path $EnvFile -Pattern "^$name=(.*)$").Matches.Groups[1].Value
}

# The address other devices on the same Wi-Fi use: the IPv4 of the adapter that has a default gateway.
$lanAddress = (Get-NetIPConfiguration | Where-Object { $_.IPv4DefaultGateway -and $_.NetAdapter.Status -eq "Up" } |
    Select-Object -First 1).IPv4Address.IPAddress

Write-Host ""
Write-Host "Website:    http://localhost"
if ($lanAddress) { Write-Host "Phone:      http://$lanAddress (same Wi-Fi as this computer)" }
Write-Host "Emails:     http://localhost:8025 (demo inbox, nothing is really sent)"
Write-Host "Staff app:  server http://localhost:8080, login $(Get-EnvValue 'APP_ADMIN_USER') / $(Get-EnvValue 'APP_ADMIN_PASSWORD')"
Write-Host "Stop it:    docker compose down"
