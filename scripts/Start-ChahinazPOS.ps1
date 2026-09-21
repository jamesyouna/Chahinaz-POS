$ErrorActionPreference='Stop'
$root=Split-Path -Parent $PSScriptRoot
Set-Location $root
if(!(Test-Path '.env.production')){throw 'Missing .env.production. Copy .env.production.example and replace every placeholder.'}
$production=@{};Get-Content '.env.production'|Where-Object{$_ -match '^[A-Z0-9_]+='}|ForEach-Object{$k,$v=$_.Split('=',2);$production[$k]=$v.Trim()}
$required='POS_VERSION','POS_DB_NAME','POS_DB_USER','POS_DB_PASSWORD','POS_BOOTSTRAP_ADMIN_USERNAME'
foreach($key in $required){if(!$production[$key]-or$production[$key]-match 'REPLACE_|temporary-test'){throw "Invalid production configuration: $key is missing or still contains a test/placeholder value."}}
if($production.POS_BOOTSTRAP_ADMIN_PASSWORD-and$production.POS_BOOTSTRAP_ADMIN_PASSWORD-match 'REPLACE_|temporary-test'){throw 'Invalid production configuration: POS_BOOTSTRAP_ADMIN_PASSWORD still contains a test/placeholder value.'}
if($production.POS_DB_NAME-match '_test$'-or$production.POS_DB_USER-match '_test$'-or$production.POS_VERSION-eq'test'-or$production.POS_BOOTSTRAP_ADMIN_USERNAME-match '^phase\d+admin$'){throw 'Refusing to start: .env.production still contains test installation identities.'}
if(!(Get-Command docker -ErrorAction SilentlyContinue)){throw 'Docker Desktop is not installed. Install and start Docker Desktop.'}
try{docker info *> $null}catch{throw 'Docker Desktop is not running. Start Docker Desktop, wait until it is ready, then try again.'}
$port=((Get-Content '.env.production'|Where-Object{$_ -match '^POS_APP_PORT='})-replace '^POS_APP_PORT=','').Trim();if(!$port){$port='8080'}
$url="http://127.0.0.1:$port"
try{$health=Invoke-RestMethod "$url/health" -TimeoutSec 2;if($health.status-eq'UP'){Start-Process $url;exit 0}}catch{}
docker compose --env-file .env.production -f compose.production.yaml up -d
if($LASTEXITCODE-ne 0){throw 'The POS containers could not start. Run docker compose logs for details.'}
for($i=0;$i-lt 60;$i++){try{$health=Invoke-RestMethod "$url/health" -TimeoutSec 2;if($health.status-eq'UP'){Start-Process $url;exit 0}}catch{};Start-Sleep -Seconds 2}
docker compose --env-file .env.production -f compose.production.yaml ps
throw "The POS did not become healthy at $url within two minutes. Check logs\chahinaz-pos.log and Docker Desktop."
