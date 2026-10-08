@echo off
setlocal
echo =======================================================================
echo   Starting Online Library Management System (Java 21 Backend + Web UI)
echo =======================================================================

cd /d "%~dp0"

if not exist "bin" mkdir "bin"

echo [1/2] Compiling Java source files...
javac -encoding UTF-8 -d bin src\com\library\Main.java src\com\library\model\*.java src\com\library\service\*.java src\com\library\util\*.java src\com\library\server\*.java
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Compilation failed!
    pause
    exit /b %ERRORLEVEL%
)

echo [2/2] Launching Java Web Server on port 8080...
echo Server running at http://localhost:8080
java -cp bin com.library.Main 8080

pause
