@echo off
title Biblosmast - Frontend
cd /d %~dp0

where node >nul 2>nul
if errorlevel 1 (
    echo No se encontro Node.js. Instala la version 20 o superior desde https://nodejs.org
    pause
    exit /b 1
)

if not exist node_modules (
    echo Instalando dependencias por primera vez...
    call npm install
    if errorlevel 1 (
        echo Fallo npm install. Revisa tu conexion a internet.
        pause
        exit /b 1
    )
)

echo.
echo Recuerda: MySQL en Laragon y la API (puerto 8094) deben estar corriendo.
echo Abriendo http://localhost:5173 ...
start "" http://localhost:5173
call npm run dev
pause
