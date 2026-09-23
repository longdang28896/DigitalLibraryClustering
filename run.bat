@echo off
chcp 65001 >nul
title He Thong Quan Ly Thu Vien So - Clustering (Java Swing)
cd /d "%~dp0"

echo =======================================================================
echo   HE THONG QUAN LY THU VIEN SO TICH HOP PHAN CUM TAI LIEU (JAVA)
echo   BAI TAP LON: NHAP MON KHAI PHA DU LIEU VA MAY HOC - DE SO 2508
echo   GIAO DIEN: DESKTOP APPLICATION (JAVA SWING)
echo =======================================================================
echo.

set "JAVA_CMD=java"
if exist "C:\Users\NLSync\jdk-17.0.20.1+1\bin\java.exe" (
    set "JAVA_CMD=C:\Users\NLSync\jdk-17.0.20.1+1\bin\java.exe"
)

echo Dang kiem tra Java...
"%JAVA_CMD%" -version
echo.

set "JAVA_OPTS=-Dfile.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8 -Dsun.stderr.encoding=UTF-8"

if exist "DigitalLibrary.jar" (
    echo [OK] Dang khoi chay DigitalLibrary.jar...
    "%JAVA_CMD%" %JAVA_OPTS% -jar DigitalLibrary.jar
    goto finished
)

if exist "target\digital-library-clustering-1.0.0-jar-with-dependencies.jar" (
    echo [OK] Dang khoi chay tu thu muc target...
    "%JAVA_CMD%" %JAVA_OPTS% -jar target\digital-library-clustering-1.0.0-jar-with-dependencies.jar
    goto finished
)

echo [LOI] Khong tim thay file JAR de chay!
echo Vui long kiem tra file DigitalLibrary.jar hoac bien dich lai bang: mvn package

:finished
echo.
echo Ung dung da ket thuc phien lam viec.
pause
