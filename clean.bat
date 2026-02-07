@echo off
echo Stopping all containers and removing volumes...
docker-compose down -v
echo.
echo Containers and volumes removed.
pause
