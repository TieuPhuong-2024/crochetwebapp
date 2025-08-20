@echo off
echo Starting NATS Docker Container...

REM Check if Docker is running
docker info >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo Error: Docker is not running. Please start Docker first.
    pause
    exit /b 1
)

REM Build and start NATS container
echo Building NATS Docker image...
docker-compose -f docker-compose.nats.yml build

echo Starting NATS server...
docker-compose -f docker-compose.nats.yml up -d

REM Wait for NATS to be ready
echo Waiting for NATS server to be ready...
timeout /t 10 /nobreak >nul

REM Check if NATS is healthy
docker-compose -f docker-compose.nats.yml ps
echo.
echo Testing NATS connection...
curl -s http://localhost:8222/healthz
if %ERRORLEVEL% EQU 0 (
    echo.
    echo ✅ NATS server is running successfully!
    echo.
    echo NATS URLs:
    echo - Client connections: nats://localhost:4222
    echo - Monitoring: http://localhost:8222
    echo - Health check: http://localhost:8222/healthz
    echo.
    echo You can now start your Spring Boot application.
) else (
    echo.
    echo ❌ NATS server health check failed.
    echo Check logs with: docker-compose -f docker-compose.nats.yml logs
)

echo.
pause
