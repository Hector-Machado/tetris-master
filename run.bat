@echo off
REM Configurações do Java
set JAVA_OPTS=--add-opens java.base/java.lang=ALL-UNNAMED --add-opens java.base/java.security=ALL-UNNAMED

REM Executa o jogo
java %JAVA_OPTS% -jar "target\tetris.jar"

REM Mantém o console aberto após execução
pause