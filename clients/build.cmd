@echo off
javac -d bin %* src\iwltas\*.java src\iwltas\cli\*.java src\iwltas\gui\*.java
pause
cd bin
jar -cf tas_tools.jar iwltas
cd ..
