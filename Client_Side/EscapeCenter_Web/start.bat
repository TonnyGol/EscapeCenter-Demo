@echo off
echo Starting EscapeCenter Web Client...
echo.
echo Checking if dependencies are installed...
if not exist "node_modules" (
    echo Installing dependencies...
    call npm install
)
echo.
echo Starting development server...
echo The app will open at http://localhost:5173
echo.
echo Press Ctrl+C to stop the server
call npm run dev
