@if "%DEBUG%" == "" @echo off
@rem ##########################################################################
@rem
@rem  Gradle startup script for Windows
@rem
@rem ##########################################################################

@rem Set local scope for the variables with windows NT shell
if "%OS%"=="Windows_NT" setlocal

@rem Add default JVM options here. You can also use JAVA_OPTS and GRADLE_OPTS to pass JVM options to this script.
set DEFAULT_JVM_OPTS=

set DIRNAME=%~dp0
if "%DIRNAME%" == "" set DIRNAME=.
set APP_BASE_NAME=%~n0
set APP_HOME=%DIRNAME%

@rem NilLoaderSDK bootstrap: Gradle 8.8 cannot run on Java 25. Ask the
@rem PowerShell finder to validate the current JAVA_HOME first (important for
@rem actions/setup-java matrix jobs), then scan installed JDKs if necessary.
set "NILSDK_SELECTED_JAVA_HOME="

@rem Explicit override
if defined NILSDK_GRADLE_JAVA_HOME (
    if exist "%NILSDK_GRADLE_JAVA_HOME%\bin\java.exe" (
        set "NILSDK_SELECTED_JAVA_HOME=%NILSDK_GRADLE_JAVA_HOME%"
    )
)

@rem GitHub Actions setup-java exposes these.
if not defined NILSDK_SELECTED_JAVA_HOME (
    if defined JAVA_HOME_21_X64 (
        if exist "%JAVA_HOME_21_X64%\bin\java.exe" (
            set "NILSDK_SELECTED_JAVA_HOME=%JAVA_HOME_21_X64%"
        )
    )
)

if not defined NILSDK_SELECTED_JAVA_HOME (
    if defined JAVA_HOME_17_X64 (
        if exist "%JAVA_HOME_17_X64%\bin\java.exe" (
            set "NILSDK_SELECTED_JAVA_HOME=%JAVA_HOME_17_X64%"
        )
    )
)

@rem Normal Windows installation fallback
if not defined NILSDK_SELECTED_JAVA_HOME (
    for /f "usebackq delims=" %%J in (`powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%APP_HOME%gradle\find-gradle-java.ps1"`) do (
        set "NILSDK_SELECTED_JAVA_HOME=%%J"
    )
)

if not defined NILSDK_SELECTED_JAVA_HOME goto noCompatibleJava
set "JAVA_HOME=%NILSDK_SELECTED_JAVA_HOME%"
set "JAVA_EXE=%JAVA_HOME%\bin\java.exe"
if exist "%JAVA_EXE%" goto init

:noCompatibleJava
echo.
echo ERROR: NilLoaderSDK could not locate a supported JDK (17-22) for Gradle 8.8.
echo Install JDK 21/17 or set NILSDK_GRADLE_JAVA_HOME.
goto fail

:init
@rem Get command-line arguments, handling Windowz variants

if not "%OS%" == "Windows_NT" goto win9xME_args
if "%@eval[2+2]" == "4" goto 4NT_args

:win9xME_args
@rem Slurp the command line arguments.
set CMD_LINE_ARGS=
set _SKIP=2

:win9xME_args_slurp
if "x%~1" == "x" goto execute

set CMD_LINE_ARGS=%*
goto execute

:4NT_args
@rem Get arguments from the 4NT Shell from JP Software
set CMD_LINE_ARGS=%$

:execute
@rem Setup the command line

set CLASSPATH=%APP_HOME%\gradle\wrapper\gradle-wrapper.jar

@rem Execute Gradle
"%JAVA_EXE%" %DEFAULT_JVM_OPTS% %JAVA_OPTS% %GRADLE_OPTS% "-Dorg.gradle.appname=%APP_BASE_NAME%" -classpath "%CLASSPATH%" org.gradle.wrapper.GradleWrapperMain %CMD_LINE_ARGS%

:end
@rem End local scope for the variables with windows NT shell
if "%ERRORLEVEL%"=="0" goto mainEnd

:fail
rem Set variable GRADLE_EXIT_CONSOLE if you need the _script_ return code instead of
rem the _cmd.exe /c_ return code!
if  not "" == "%GRADLE_EXIT_CONSOLE%" exit 1
exit /b 1

:mainEnd
if "%OS%"=="Windows_NT" endlocal

:omega
