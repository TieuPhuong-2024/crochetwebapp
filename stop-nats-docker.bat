@echo off
echo Stopping NATS Docker Container...

docker-compose -f docker-compose.nats.yml down
if %ERRORLEVEL% EQU 0 (
    echo ✅ NATS container stopped successfully
) else (
    echo ❌ Failed to stop NATS container
)

echo.
echo Docker container status:
docker-compose -f docker-compose.nats.yml ps
echo.

pause
