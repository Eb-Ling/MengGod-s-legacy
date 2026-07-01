@echo off
cd /d "%~dp0"

set JAVA_EXE=java
set LIB_PATH=%~dp0lib
set NATIVE_PATH=%LIB_PATH%\native\windows
set JAR_FILE=%~dp0MengGL11Tool.jar
set DATA_PATH=%~dp0data

if not exist "%JAR_FILE%" (
    echo ERROR: JAR file not found: %JAR_FILE%

    echo Please build artifact in IDEA first
    pause
    exit /b 1
)

if not exist "%LIB_PATH%\lwjgl.jar" (
    echo ERROR: Required libraries not found in lib folder
    echo Please ensure lib folder contains lwjgl.jar and lwjgl_util.jar
    pause
    exit /b 1
)

REM Create data directory if not exists
if not exist "%DATA_PATH%" (
    mkdir "%DATA_PATH%"
    echo Created data directory: %DATA_PATH%
)

REM Try to find Java automatically if not in PATH
where java >nul 2>&1
if errorlevel 1 (
    echo Java not found in PATH, searching common locations...

    if exist "C:\Program Files\Java\jdk-17\bin\java.exe" (
        set JAVA_EXE=C:\Program Files\Java\jdk-17\bin\java.exe
        echo Found Java 17 at: %JAVA_EXE%
    ) else if exist "C:\Program Files\Java\jdk-21\bin\java.exe" (
        set JAVA_EXE=C:\Program Files\Java\jdk-21\bin\java.exe
        echo Found Java 21 at: %JAVA_EXE%
    ) else if exist "C:\Program Files\Java\jdk1.8.0_391\bin\java.exe" (
        set JAVA_EXE=C:\Program Files\Java\jdk1.8.0_391\bin\java.exe
        echo Found Java 8 at: %JAVA_EXE%
    ) else (
        for /d %%i in ("C:\Program Files\Java\jdk*") do (
            if exist "%%i\bin\java.exe" (
                set JAVA_EXE=%%i\bin\java.exe
                echo Found Java at: !JAVA_EXE!
                goto :found_java
            )
        )

        for /d %%i in ("%USERPROFILE%\AppData\Local\Programs\BellSoft\LibericaJDK*") do (
            if exist "%%i\bin\java.exe" (
                set JAVA_EXE=%%i\bin\java.exe
                echo Found Liberica JDK at: !JAVA_EXE!
                goto :found_java
            )
        )

        echo ERROR: Java not found!
        echo Please install JDK 17 or higher from https://adoptium.net/
        pause
        exit /b 1
    )
)

:found_java
echo Starting MyRender Tool...
echo Using Java: %JAVA_EXE%
echo Data directory: %DATA_PATH%
echo Auto-compilation: Enabled for .java files
echo.

%JAVA_EXE% ^
    --add-opens java.base/java.lang=ALL-UNNAMED ^
    --add-opens java.base/sun.misc=ALL-UNNAMED ^
    --add-opens java.desktop/java.awt=ALL-UNNAMED ^
    --add-opens java.desktop/sun.awt=ALL-UNNAMED ^
    --add-opens java.desktop/sun.font=ALL-UNNAMED ^
    --add-opens java.desktop/java.awt.image=ALL-UNNAMED ^
    -Djava.library.path="%NATIVE_PATH%" ^
    -cp "%DATA_PATH%;%JAR_FILE%;%LIB_PATH%\lwjgl.jar;%LIB_PATH%\lwjgl_util.jar" ^
    data.RenderTestMain

if errorlevel 1 (
    echo.
    echo ERROR occurred!
    pause
)
