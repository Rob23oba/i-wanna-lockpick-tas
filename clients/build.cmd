@echo off
rmdir /S /Q bin
javac -d bin %* src\iwltas\*.java src\iwltas\cli\*.java src\iwltas\gui\*.java
copy /B src\iwltas\gui\masks.bin bin\iwltas\gui > nul
pause
cd bin
jar -cf tas_tools.jar iwltas
cd ..
