@echo off
setlocal
pushd "%~dp0"
if not "%~1"=="" (
    call gradlew.bat -p ports "-Ptarget=%~1" build --console=plain
    if errorlevel 1 goto failed
    goto success
)
for %%V in (26.1 26.1.1 26.1.2 26.2 26.3) do (
    call gradlew.bat -p ports "-Ptarget=%%V" build --console=plain
    if errorlevel 1 goto failed
)
:success
popd
exit /b 0
:failed
popd
exit /b 1
