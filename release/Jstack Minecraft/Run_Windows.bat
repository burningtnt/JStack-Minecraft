%1 start "" mshta vbscript:CreateObject("Shell.Application").ShellExecute("cmd.exe","/c pushd ""%~dp0"" && ""%~s0"" ::","","runas",1)(window.close)&&exit
cd %~dp0
jre_Windows\bin\java.exe -jar "Jstack-Minecraft.jar"
pause
