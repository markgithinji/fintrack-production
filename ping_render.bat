@echo off
set "URL=https://fintrack-ktor.onrender.com"
title FinTrack Render Keep-Warm

echo ======================================================
echo Pinging %URL% every 10 minutes.
echo Keep this window open to prevent Render spin-down.
echo ======================================================

:loop
echo [%time%] Sending keep-alive request to Render...
curl -I -s %URL% | findstr "HTTP/"
echo [%time%] Activity recorded. (Note: 404 is normal and keeps the server awake)
timeout /t 600 /nobreak
goto loop
