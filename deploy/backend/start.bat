@echo off
rem 后端启动脚本（Windows）— 外置配置放在 config\application.yml
cd /d "%~dp0"
set "JAR=%~dp0app.jar"
set "CONFIG=%~dp0config\application.yml"
set "LOG=%~dp0logs\app.log"
if not exist "%~dp0logs" mkdir "%~dp0logs"
set "CONFIG_ARG="
if exist "%CONFIG%" set "CONFIG_ARG=--spring.config.additional-location=file:%CONFIG%"

java -Xms512m -Xmx1024m -Dfile.encoding=UTF-8 -Duser.timezone=Asia/Shanghai -jar "%JAR%" %CONFIG_ARG%
