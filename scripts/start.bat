cd "%~dp0"
call .\param.bat
java.exe -Dlog4j.configuration=file:log4j.properties -Dlog4j.watch=5000 -Dmail.smtp.timeout=30000 -Dmail.smtp.connectiontimeout=30000 psn.filechief.FileChief start

