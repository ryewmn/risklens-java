@echo off
set MVN_VERSION=3.9.9
set BASE=%USERPROFILE%\.m2\wrapper\dists\apache-maven-%MVN_VERSION%
set MVN=%BASE%\apache-maven-%MVN_VERSION%\bin\mvn.cmd
if not exist "%MVN%" (
  powershell -NoProfile -Command "$u='https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/%MVN_VERSION%/apache-maven-%MVN_VERSION%-bin.zip'; New-Item -Force -ItemType Directory '%BASE%' | Out-Null; Invoke-WebRequest $u -OutFile '%BASE%\maven.zip'; Expand-Archive -Force '%BASE%\maven.zip' '%BASE%'; Remove-Item '%BASE%\maven.zip'"
)
call "%MVN%" %*
