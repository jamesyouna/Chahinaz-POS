param([string]$EnvironmentFile='')
$ErrorActionPreference='Stop';$root=Split-Path -Parent $PSScriptRoot;Set-Location $root;if(!$EnvironmentFile){$EnvironmentFile=Join-Path $root '.env.production'}
if(!(Test-Path $EnvironmentFile)){throw "Missing $EnvironmentFile"}
$cfg=@{};Get-Content $EnvironmentFile|Where-Object{$_ -match '^[A-Z0-9_]+='}|ForEach-Object{$k,$v=$_.Split('=',2);$cfg[$k]=$v}
$backupRoot=$cfg.POS_BACKUP_DIR;if(!$backupRoot){$backupRoot=Join-Path $root 'backups'};$daily=Join-Path $backupRoot 'daily';$monthly=Join-Path $backupRoot 'monthly';New-Item -ItemType Directory -Force $daily,$monthly|Out-Null
$stamp=Get-Date -Format 'yyyy-MM-dd-HHmmss';$file=Join-Path $daily "chahinaz-pos-$stamp.dump";$tmp="$file.partial"
docker compose --env-file $EnvironmentFile -f compose.production.yaml exec -T postgres sh -c 'PGPASSWORD="$POSTGRES_PASSWORD" pg_dump -U "$POSTGRES_USER" -d "$POSTGRES_DB" -Fc -f /tmp/chahinaz-backup.dump'
if($LASTEXITCODE-eq 0){docker cp chahinaz-pos-production-postgres:/tmp/chahinaz-backup.dump $tmp;docker compose --env-file $EnvironmentFile -f compose.production.yaml exec -T postgres rm -f /tmp/chahinaz-backup.dump}
if($LASTEXITCODE-ne 0 -or !(Test-Path $tmp) -or (Get-Item $tmp).Length-lt 1024){Remove-Item $tmp -ErrorAction SilentlyContinue;throw 'pg_dump failed or produced an invalidly small archive.'}
Move-Item $tmp $file
docker cp $file chahinaz-pos-production-postgres:/tmp/chahinaz-verify.dump;docker compose --env-file $EnvironmentFile -f compose.production.yaml exec -T postgres pg_restore -l /tmp/chahinaz-verify.dump *> $null;$verifyExit=$LASTEXITCODE;docker compose --env-file $EnvironmentFile -f compose.production.yaml exec -T postgres rm -f /tmp/chahinaz-verify.dump
if($verifyExit-ne 0){Remove-Item $file -ErrorAction SilentlyContinue;throw 'pg_restore could not inspect the new archive.'}
if((Get-Date).Day-eq 1){Copy-Item $file (Join-Path $monthly (Split-Path $file -Leaf))}
$keepDaily=30;if($cfg.POS_BACKUP_RETENTION_DAILY){$keepDaily=[int]$cfg.POS_BACKUP_RETENTION_DAILY};$keepMonthly=12;if($cfg.POS_BACKUP_RETENTION_MONTHLY){$keepMonthly=[int]$cfg.POS_BACKUP_RETENTION_MONTHLY};Get-ChildItem $daily -Filter '*.dump'|Sort-Object LastWriteTime -Descending|Select-Object -Skip $keepDaily|Remove-Item;Get-ChildItem $monthly -Filter '*.dump'|Sort-Object LastWriteTime -Descending|Select-Object -Skip $keepMonthly|Remove-Item
Write-Host "Verified backup created: $file" -ForegroundColor Green
