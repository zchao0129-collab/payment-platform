@echo off
rem 一键打包脚本（Windows）：构建后端 jar + 前端 dist，组装到 release/
setlocal
set "ROOT=%~dp0"
set "RELEASE=%ROOT%release"

echo ==^> 清理 release/
if exist "%RELEASE%" rmdir /s /q "%RELEASE%"
mkdir "%RELEASE%\backend\config"
mkdir "%RELEASE%\frontend"

echo ==^> [1/4] 构建后端 (mvn clean package -DskipTests)
cd /d "%ROOT%"
call mvn clean package -DskipTests -q
if errorlevel 1 goto :err
copy /y "%ROOT%target\payment-platform-1.0.0.jar" "%RELEASE%\backend\app.jar" >nul

echo ==^> [2/4] 复制外置配置与启停脚本
copy /y "%ROOT%deploy\config\application.yml.example" "%RELEASE%\backend\config\application.yml.example" >nul
copy /y "%ROOT%deploy\backend\start.sh" "%RELEASE%\backend\" >nul
copy /y "%ROOT%deploy\backend\stop.sh" "%RELEASE%\backend\" >nul
copy /y "%ROOT%deploy\backend\start.bat" "%RELEASE%\backend\" >nul

echo ==^> [3/4] 构建前端 (npm run build)
cd /d "%ROOT%frontend"
call npm run build
if errorlevel 1 goto :err
xcopy /e /i /y "%ROOT%frontend\dist" "%RELEASE%\frontend\" >nul
copy /y "%ROOT%frontend\nginx.conf.example" "%RELEASE%\frontend\" >nul

echo ==^> [4/4] 复制说明
copy /y "%ROOT%deploy\README.md" "%RELEASE%README.md" >nul

echo.
echo 打包完成: %RELEASE%
exit /b 0

:err
echo.
echo 构建失败！
exit /b 1
