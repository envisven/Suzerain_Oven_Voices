@echo off
rem Called by build/run/test; never installs or downloads a runtime.
if defined JAVA_HOME (
  set "JDK_BIN=%JAVA_HOME%\bin"
) else (
  for /f "delims=" %%J in ('where javac.exe 2^>nul') do if not defined JDK_BIN set "JDK_BIN=%%~dpJ"
)
if not defined JDK_BIN (
  echo Set JAVA_HOME to your existing Liberica JDK 25 FULL installation. 1>&2
  exit /b 1
)
if not exist "%JDK_BIN%\javac.exe" (
  echo JAVA_HOME must point to a full JDK with java and javac. 1>&2
  exit /b 1
)
"%JDK_BIN%\java.exe" --list-modules | findstr /b /c:"javafx.controls@" >nul
if errorlevel 1 (
  echo JavaFX is missing. Select Liberica JDK 25 FULL. 1>&2
  exit /b 1
)
exit /b 0

