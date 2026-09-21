@echo off
cd /d "%~dp0"
docker compose --env-file .env.production -f compose.production.yaml stop
if errorlevel 1 pause
