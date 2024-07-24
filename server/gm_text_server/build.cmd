@echo off

if not defined DevEnvDir (
  rem Change this to your visual studio install location
  call "C:\Program Files\Microsoft Visual Studio\2022\Community\VC\Auxiliary\Build\vcvars32.bat"
)

if not exist ..\build mkdir ..\build

cl /nologo /W3 /O2 /LD /MT /arch:IA32 /D_USRDLL /D_WINDLL extension.c /link ws2_32.lib /DLL /OUT:..\build\gm_text_server.dll
pause
