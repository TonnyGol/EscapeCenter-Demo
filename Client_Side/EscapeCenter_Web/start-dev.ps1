Write-Host "Starting EscapeCenter Web Client..." -ForegroundColor Cyan
Write-Host ""
Write-Host "Checking if dependencies are installed..." -ForegroundColor Yellow

if (-Not (Test-Path "node_modules")) {
    Write-Host "Installing dependencies..." -ForegroundColor Yellow
    npm install
}

Write-Host ""
Write-Host "Starting development server..." -ForegroundColor Green
Write-Host "The app will open at http://localhost:5173" -ForegroundColor Green
Write-Host ""
Write-Host "Press Ctrl+C to stop the server" -ForegroundColor Yellow

npm run dev
