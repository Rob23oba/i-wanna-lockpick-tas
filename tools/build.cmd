@echo off

if not defined DevEnvDir (
  rem Change this to your visual studio install location
  call "C:\Program Files\Microsoft Visual Studio\2022\Community\VC\Auxiliary\Build\vcvars64.bat"
)

if not exist bin mkdir bin

pushd src
cl /W3 /O2 jump_calculator.c /Fo:..\bin\ /link /out:..\bin\jump_calculator.exe
popd
