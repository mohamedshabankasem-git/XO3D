@echo off
setlocal
set V=8.9
set D=%USERPROFILE%\.gradle\wrapper\dists\gradle-%V%-bin\xo3d
set G=%D%\gradle-%V%\bin\gradle.bat
if not exist "%G%" (
 if not exist "%D%" mkdir "%D%"
 powershell -NoProfile -Command "Invoke-WebRequest https://services.gradle.org/distributions/gradle-%V%-bin.zip -OutFile '%D%\gradle.zip'; Expand-Archive -Force '%D%\gradle.zip' '%D%'"
)
call "%G%" -p "%~dp0" %*
