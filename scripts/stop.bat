cd "%~dp0"
call .\param.bat
java.exe -Dlog4j.configuration=file:log4j.stop -Dlog4j.watch=5000 psn.filechief.FileChief stop

