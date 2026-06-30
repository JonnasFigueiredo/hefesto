<#
.SYNOPSIS
  Inicializa o Hefesto: builda o backend (Spring Boot) e o frontend (Vite),
  e sobe os dois. Resolve JDK, Node e Maven automaticamente nesta maquina.

.DESCRIPTION
  Backend em http://localhost:8080  (MCP em /sse)
  Frontend em http://localhost:5173

.PARAMETER Rebuild
  Forca o repackage do backend mesmo que o jar ja exista.

.PARAMETER Tests
  Roda os testes no build do backend (por padrao sao pulados).

.PARAMETER BackendOnly
  Sobe apenas o backend.

.PARAMETER FrontendOnly
  Sobe apenas o frontend.

.PARAMETER BuildOnly
  Builda mas nao sobe nada.

.EXAMPLE
  .\start.ps1
  .\start.ps1 -Rebuild
  .\start.ps1 -BackendOnly -Tests
#>
[CmdletBinding()]
param(
  [switch]$Rebuild,
  [switch]$Tests,
  [switch]$BackendOnly,
  [switch]$FrontendOnly,
  [switch]$BuildOnly
)

$ErrorActionPreference = 'Stop'
$root        = $PSScriptRoot
$backendDir  = Join-Path $root 'hefesto-backend'
$frontendDir = Join-Path $root 'hefesto-frontend'
$toolsDir    = Join-Path $root '.tools'
$jarRel      = 'target\hefesto-backend-0.1.0-SNAPSHOT.jar'
$jar         = Join-Path $backendDir $jarRel
$localYml    = Join-Path $backendDir 'src\main\resources\application-local.yml'
$tokenFile   = Join-Path $env:USERPROFILE '.claude-oauth-token'

# Defaults desta maquina (sobrescreva com as env vars correspondentes)
$jdkDefault   = 'C:\Program Files\Java\jdk-17.0.19'
$nodeDefault  = 'C:\Program Files\nodejs'
$mvnVersion   = '3.9.9'
$mvnUrl       = "https://archive.apache.org/dist/maven/maven-3/$mvnVersion/binaries/apache-maven-$mvnVersion-bin.zip"

function Info($m){ Write-Host "[hefesto] $m" -ForegroundColor Cyan }
function Ok($m)  { Write-Host "[hefesto] $m" -ForegroundColor Green }
function Warn($m){ Write-Host "[hefesto] $m" -ForegroundColor Yellow }
function Die($m) { Write-Host "[hefesto] $m" -ForegroundColor Red; exit 1 }

# ---------------------------------------------------------------- JDK
function Resolve-Java {
  if ($env:JAVA_HOME -and (Test-Path (Join-Path $env:JAVA_HOME 'bin\java.exe'))) {
    return (Join-Path $env:JAVA_HOME 'bin\java.exe')
  }
  if (Test-Path (Join-Path $jdkDefault 'bin\java.exe')) {
    $env:JAVA_HOME = $jdkDefault
    return (Join-Path $jdkDefault 'bin\java.exe')
  }
  $onPath = (Get-Command java -ErrorAction SilentlyContinue)
  if ($onPath) { return $onPath.Source }
  Die "JDK 17 nao encontrado. Defina JAVA_HOME ou ajuste `$jdkDefault no script."
}

# ---------------------------------------------------------------- Node
function Resolve-Node {
  if (-not (Get-Command node -ErrorAction SilentlyContinue)) {
    if (Test-Path (Join-Path $nodeDefault 'node.exe')) {
      $env:PATH = "$nodeDefault;$env:PATH"
    } else {
      Die "Node nao encontrado. Instale ou ajuste `$nodeDefault no script."
    }
  }
  Info ("Node " + (node --version))
}

# ---------------------------------------------------------------- Maven
function Resolve-Maven {
  $onPath = Get-Command mvn -ErrorAction SilentlyContinue
  if ($onPath) { return $onPath.Source }

  $cached = Join-Path $toolsDir "apache-maven-$mvnVersion\bin\mvn.cmd"
  if (Test-Path $cached) { return $cached }

  Warn "Maven nao encontrado. Baixando $mvnVersion para .tools (uma vez so)..."
  New-Item -ItemType Directory -Force -Path $toolsDir | Out-Null
  $zip = Join-Path $toolsDir "maven.zip"
  # --ssl-no-revoke por causa da intercepcao de TLS (AVG) nesta maquina
  & curl.exe -L --ssl-no-revoke -o $zip $mvnUrl
  if (-not (Test-Path $zip)) { Die "Falha ao baixar o Maven." }
  Expand-Archive -Path $zip -DestinationPath $toolsDir -Force
  Remove-Item $zip -Force
  if (-not (Test-Path $cached)) { Die "Maven baixado mas mvn.cmd nao foi encontrado." }
  Ok "Maven instalado em .tools"
  return $cached
}

# ---------------------------------------------------------------- Build backend
function Build-Backend($mvn, $java) {
  $needBuild = $Rebuild -or (-not (Test-Path $jar))
  if (-not $needBuild) { Info "Jar do backend ja existe (use -Rebuild para refazer)."; return }

  Info "Encerrando java.exe antigo (se houver) para liberar o jar..."
  Get-Process java -ErrorAction SilentlyContinue | Stop-Process -Force -ErrorAction SilentlyContinue

  # Flags de TLS para o resolver do Maven (intercepcao de TLS nesta maquina)
  $tls = @(
    '-Dmaven.resolver.transport=wagon',
    '-Dmaven.wagon.http.ssl.insecure=true',
    '-Dmaven.wagon.http.ssl.allowall=true',
    '-Dmaven.wagon.http.ssl.ignore.validity.dates=true'
  )
  $goals = @('package')
  if (-not $Tests) { $goals += '-DskipTests' }

  Info "Buildando o backend (mvn $($goals -join ' '))..."
  Push-Location $backendDir
  try {
    $env:JAVA_HOME = Split-Path (Split-Path $java)
    & $mvn @tls @goals
    if ($LASTEXITCODE -ne 0) { Die "Build do backend falhou." }
  } finally { Pop-Location }
  Ok "Backend buildado."
}

# ---------------------------------------------------------------- Frontend deps
function Ensure-FrontendDeps {
  if (-not (Test-Path (Join-Path $frontendDir 'node_modules'))) {
    Info "Instalando dependencias do frontend (npm install)..."
    Push-Location $frontendDir
    try {
      & npm install
      if ($LASTEXITCODE -ne 0) { Die "npm install falhou." }
    } finally { Pop-Location }
  }
}

# ---------------------------------------------------------------- Run
function Start-Backend($java) {
  if (-not (Test-Path $jar)) { Die "Jar nao encontrado. Rode com -Rebuild." }

  $tokenLine = ''
  if (Test-Path $tokenFile) {
    $tokenLine = "`$env:CLAUDE_CODE_OAUTH_TOKEN=(Get-Content -Raw '$tokenFile').Trim(); "
  } else {
    Warn "Token do claude-code nao encontrado em $tokenFile (as features de IA com claude-code podem falhar)."
  }

  $localArg = ''
  if (Test-Path $localYml) {
    $localArg = " --spring.config.additional-location=optional:file:./src/main/resources/application-local.yml"
  }

  # -Djavax.net.ssl.trustStoreType=Windows-ROOT: usa o truststore do Windows
  # (confia no cert do interceptador de TLS) para as chamadas HTTPS ao Jira.
  $cmd = "$tokenLine& '$java' -Djavax.net.ssl.trustStoreType=Windows-ROOT -jar '$jarRel'$localArg"
  Info "Subindo o backend em http://localhost:8080 (nova janela)..."
  Start-Process powershell -WorkingDirectory $backendDir -ArgumentList '-NoExit','-Command', $cmd
}

function Start-Frontend {
  Info "Subindo o frontend em http://localhost:5173 (nova janela)..."
  Start-Process powershell -WorkingDirectory $frontendDir -ArgumentList '-NoExit','-Command','npm run dev'
}

# ================================================================ main
Info "Raiz: $root"
$java = Resolve-Java
Info "Java: $java"

if (-not $FrontendOnly) {
  $mvn = Resolve-Maven
  Info "Maven: $mvn"
  Build-Backend $mvn $java
}
if (-not $BackendOnly) {
  Resolve-Node
  Ensure-FrontendDeps
}

if ($BuildOnly) { Ok "Build concluido (BuildOnly)."; exit 0 }

if (-not $FrontendOnly) { Start-Backend $java }
if (-not $BackendOnly)  { Start-Frontend }

Ok "Pronto."
Write-Host ""
Write-Host "  Frontend : http://localhost:5173"
Write-Host "  Backend  : http://localhost:8080   (MCP SSE: http://localhost:8080/sse)"
Write-Host ""
Write-Host "  Registrar o MCP no Claude Code:"
Write-Host "    claude mcp add --transport sse hefesto http://localhost:8080/sse"
