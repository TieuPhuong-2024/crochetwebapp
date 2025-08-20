@echo off
echo Testing NATS Docker Connection...

REM Test NATS health endpoint
echo Testing NATS health endpoint...
curl -s http://localhost:8222/healthz
if %ERRORLEVEL% EQU 0 (
    echo ✅ NATS health check passed
) else (
    echo ❌ NATS health check failed
)

echo.
echo Testing NATS monitoring endpoints...
echo.

REM Test JetStream info
echo JetStream Info:
curl -s http://localhost:8222/jsz | findstr /r /c:".*"
echo.

REM Test connections info
echo Active Connections:
curl -s http://localhost:8222/connz | findstr /r /c:".*"
echo.

echo Docker container status:
docker-compose -f docker-compose.nats.yml ps
echo.

echo Container logs (last 10 lines):
docker-compose -f docker-compose.nats.yml logs --tail=10 nats
echo.

echo Test completed.
pause
