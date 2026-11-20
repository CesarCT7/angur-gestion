@echo off
setlocal
cd /d "%~dp0"
chcp 65001 >nul
if not exist "scripts\IniciarAngur.java" (
    echo Extrae TODO el ZIP a una carpeta antes de abrir este archivo.
    pause
    exit /b 1
)
if not exist ".tmp" mkdir ".tmp"
set "ANGUR_JAVA=java"
if defined JAVA_HOME if exist "%JAVA_HOME%\bin\java.exe" set "ANGUR_JAVA=%JAVA_HOME%\bin\java.exe"
"%ANGUR_JAVA%" -version > ".tmp\arranque.log" 2>&1
if errorlevel 1 (
    echo Instala Java 21 o un JDK 21 y vuelve a abrir este archivo.
    type ".tmp\arranque.log"
    pause
    exit /b 1
)
echo Iniciando Angur Gestion. Deja esta ventana abierta.
echo Se comprobara MySQL antes de abrir http://localhost:18080/login
echo El detalle queda en .tmp\arranque.log
if exist "scripts\iniciador.jar" (
    "%ANGUR_JAVA%" -jar "scripts\iniciador.jar" --jar %* >> ".tmp\arranque.log" 2>&1
) else (
    "%ANGUR_JAVA%" "scripts\IniciarAngur.java" %* >> ".tmp\arranque.log" 2>&1
)
set "ANGUR_RESULT=%ERRORLEVEL%"
if not "%ANGUR_RESULT%"=="0" (
    echo No se pudo iniciar. Copia este mensaje para revisar el problema:
    type ".tmp\arranque.log"
    echo Revisa Java 21, MySQL y la configuracion de .env.
    pause
)
exit /b %ANGUR_RESULT%
