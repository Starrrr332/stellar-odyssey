@echo off
setlocal EnableDelayedExpansion
title Stellar Odyssey - build + install Prism

cd /d "%~dp0.."

rem ============================================================
rem 1) JDK 25 (auto-provisionado por Gradle en .gradle\jdks)
rem ============================================================
set "JDK25=%USERPROFILE%\.gradle\jdks\eclipse_adoptium-25-amd64-windows.2"
if exist "%JDK25%\bin\java.exe" goto :jdk_ok
set "JDK25="
for /d %%D in ("%USERPROFILE%\.gradle\jdks\*") do (
  if exist "%%D\bin\java.exe" set "JDK25=%%D"
)
:jdk_ok
if not exist "%JDK25%\bin\java.exe" (
  echo [ERROR] No se encontro JDK 25 en %USERPROFILE%\.gradle\jdks
  echo         Instala Java 25 o edita la variable JDK25 de este script.
  pause
  exit /b 1
)
set "JAVA_HOME=%JDK25%"
set "PATH=%JDK25%\bin;%PATH%"
echo [1/4] Usando JDK: %JDK25%

rem ============================================================
rem 2) Compilar common + fabric + neoforge (incluye 34 tests)
rem ============================================================
echo.
echo [2/4] Compilando: gradlew build --console=plain
call gradlew.bat build --console=plain
if errorlevel 1 (
  echo.
  echo [ERROR] El build fallo. Revisa los errores de arriba.
  pause
  exit /b 1
)

rem ============================================================
rem 3) Localizar el jar final de Fabric (sin -dev ni -sources)
rem    y verificar que lleva los features de worldgen dentro
rem ============================================================
set "JAR="
for %%f in ("fabric\build\libs\stellarodyssey-fabric-*.jar") do (
  set "CAND=%%~nxf"
  set "CHK1=!CAND:-dev.jar=!"
  set "CHK2=!CAND:-sources.jar=!"
  if "x!CHK1!"=="x!CAND!" if "x!CHK2!"=="x!CAND!" set "JAR=%%f"
)
if not defined JAR (
  echo [ERROR] No se encontro el jar en fabric\build\libs
  pause
  exit /b 1
)
echo.
echo [3/4] Jar: !JAR!
where tar >nul 2>nul
if not errorlevel 1 (
  tar -tf "!JAR!" | findstr /c:"worldgen/feature/" >nul
  if errorlevel 1 (
    echo [ERROR] Los JSON de worldgen/feature NO estan dentro del jar.
    pause
    exit /b 1
  )
  echo [OK] worldgen/feature dentro del jar verificado.
)

rem ============================================================
rem 4) Instalar en las 6 carpetas de mods de Prism + Downloads
rem ============================================================
echo.
echo [4/4] Instalando jar en la instancia de Prism...
set /a COUNT=0
for %%P in ("C:\Users\amaro\Downloads\Prism Launcher\instances\Stellar Odyssey" "C:\Users\amaro\AppData\Roaming\PrismLauncher\instances\Stellar Odyssey") do (
  if exist "%%~P" (
    for %%S in ("mods" "minecraft\mods" ".minecraft\mods") do (
      if not exist "%%~P\%%~S" mkdir "%%~P\%%~S"
      del /q "%%~P\%%~S\stellarodyssey*.jar" 2>nul
      copy /y "!JAR!" "%%~P\%%~S\" >nul
      set /a COUNT+=1
      echo   [OK] %%~P\%%~S
    )
  ) else (
    echo   [AVISO] No existe: %%~P
  )
)
copy /y "!JAR!" "C:\Users\amaro\Downloads\" >nul

echo.
echo ============================================================
echo  Listo: copiado en !COUNT! carpetas de mods + Downloads.
echo  Arranca "Stellar Odyssey" en Prism y crea un mundo nuevo.
echo ============================================================
pause
