@REM ----------------------------------------------------------------------------
@REM Maven Start Up Batch script
@REM ----------------------------------------------------------------------------

@if "%DEBUG%" == "" @echo off
@setlocal

set ERROR_CODE=0

@REM To isolate internal variables from possible post scripts, we use a localized environment
set MAVEN_PROJECTBASEDIR=%MAVEN_BASEDIR%
if "%MAVEN_PROJECTBASEDIR%"=="" set MAVEN_PROJECTBASEDIR=%~dp0

if not "%JAVA_HOME%" == "" goto OkJHome

echo.
echo ERROR: JAVA_HOME is not set in your environment.
echo Please set the JAVA_HOME variable in your environment to match the
echo location of your Java installation.
echo.
goto error

:OkJHome
if exist "%JAVA_HOME%\bin\java.exe" goto init

echo.
echo ERROR: JAVA_HOME is set to an invalid directory.
echo JAVA_HOME = "%JAVA_HOME%"
echo Please set the JAVA_HOME variable in your environment to match the
echo location of your Java installation.
echo.
goto error

:init
@REM Find the project base dir, i.e. the dir that contains the folder ".mvn".
set BASE_DIR=%~dp0

set MAVENW_JAR="%BASE_DIR%\.mvn\wrapper\maven-wrapper.jar"
set MAVENW_SETTINGS="%BASE_DIR%\.mvn\wrapper\settings.xml"

@REM Download wrapper jar if missing
if exist %MAVENW_JAR% goto run

echo Downloading Maven Wrapper...
powershell -Command "[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; (New-Object Net.WebClient).DownloadFile('https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.2.0/maven-wrapper-3.2.0.jar', %MAVENW_JAR%)"

:run
"%JAVA_HOME%\bin\java.exe" -jar %MAVENW_JAR% %*
if ERRORLEVEL 1 goto error
goto end

:error
set ERROR_CODE=1

:end
@endlocal & set ERROR_CODE=%ERROR_CODE%
cmd /c exit /b %ERROR_CODE%
