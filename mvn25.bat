@echo off
REM C:\Program Files\Microsoft\jdk-25.0.2.10-hotspot
set JAVA_HOME=C:\Program Files\Microsoft\jdk-25.0.2.10-hotspot
set PATH=%JAVA_HOME%\bin;%PATH%
echo Använder Java från: %JAVA_HOME%
java -version