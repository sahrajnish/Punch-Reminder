@rem
@rem Punch Reminder Gradle start-up script for Windows
@rem

@if "%DEBUG%"=="" @echo off
@rem Set local scope for the variables with Windows NT shell
if "%OS%"=="Windows_NT" setlocal

set DIRNAME=%~dp0
if "%DIRNAME%"=="" set DIRNAME=.
set APP_BASE_NAME=%~n0
set APP_HOME=%DIRNAME%

set DEFAULT_JVM_OPTS="-Xmx64m" "-Xms64m"

rem Find java.exe
if defined JAVA_HOME goto findJavaFromJavaHome

set JAVA_EXE=java.exe
%JAVA_EXE% -version >NUL 2>&1
if "%ERRORLEVEL%"=="0" goto init

echo.
echo ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH.
echo.
goto end

:findJavaFromJavaHome
set JAVA_HOME=%JAVA_HOME:"=%
set JAVA_EXE=%JAVA_HOME%\bin\java.exe
if exist "%JAVA_EXE%" goto init

echo.
echo ERROR: JAVA_HOME is set to an invalid directory: %JAVA_HOME%
echo.
goto end

:init
rem Get command-line arguments, handling Windows variants
if not "%OS%"=="Windows_NT" goto win9x_me_args

:win9x_me_args
rem Slurp the command line arguments.
set CMD_LINE_ARGS=
set _skip=0

:getarg
if "%0"=="" goto done
set _arg=%0
if "%_arg%"=="--skip" goto skip
set CMD_LINE_ARGS=%*

:skip
shift
goto getarg

:done

rem Set GRADLE_USER_HOME if not set
if not defined GRADLE_USER_HOME set GRADLE_USER_HOME=%USERPROFILE%\.gradle

set DISTRIBUTION_HOME=%GRADLE_USER_HOME%\wrapper\dists\gradle-8.2
set ANDROID_HOME=%ANDROID_SDK_ROOT%
if not defined ANDROID_HOME set ANDROID_HOME=%ANDROID_SDK_ROOT%

rem Download Gradle if missing
if not exist "%DISTRIBUTION_HOME%\bin\gradle.bat" (
    echo Downloading Gradle 8.2 (one time)...
    powershell -NoProfile -Command "Invoke-WebRequest -Uri 'https://services.gradle.org/distributions/gradle-8.2-bin.zip' -OutFile '%TEMP%\gradle-8.2-bin.zip'; Expand-Archive -Path '%TEMP%\gradle-8.2-bin.zip' -DestinationPath '%GRADLE_USER_HOME%\wrapper\dists'"
)

endlocal .
if "%OS%"=="Windows_NT" endlocal

call "%DISTRIBUTION_HOME%\bin\gradle.bat" --gradle-user-home "%GRADLE_USER_HOME%" %*

:end
