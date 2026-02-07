@echo off
echo ==========================================
echo Starting Moderation Service Docker Stack
echo ==========================================
echo.
echo This will start:
echo   - Zookeeper (port 2181)
echo   - Kafka (port 9092, 29092)
echo   - Kafka UI (port 8090)
echo   - MongoDB (port 27017)
echo   - Mongo Express (port 8091)
echo   - Redis (port 6379)
echo   - Redis Commander (port 8092)
echo   - Service-1 Moderation (port 8080)
echo   - Service-2 Enrichment (port 8081)
echo.
echo Press any key to start...
pause > nul

docker-compose up --build

echo.
echo ==========================================
echo Stack stopped
echo ==========================================
pause
