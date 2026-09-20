# Loads only the known local development variables. Keep .env out of Git.
$envFile = Join-Path $PSScriptRoot '..\.env'
if (-not (Test-Path -LiteralPath $envFile)) { throw 'Create .env from .env.example first.' }
$allowed = @('POS_DB_NAME','POS_DB_HOST','POS_DB_PORT','POS_DB_USER','POS_DB_PASSWORD','POS_BOOTSTRAP_ADMIN_USERNAME','POS_BOOTSTRAP_ADMIN_PASSWORD','POS_SECURE_COOKIE')
foreach ($line in Get-Content -LiteralPath $envFile) {
  $entry = $line.Trim()
  if (-not $entry -or $entry.StartsWith('#')) { continue }
  $parts = $entry.Split('=', 2)
  if ($parts.Count -ne 2 -or $parts[0] -notin $allowed) { throw "Unexpected .env key: $($parts[0])" }
  [Environment]::SetEnvironmentVariable($parts[0], $parts[1], 'Process')
}
if ($env:POS_DB_PASSWORD -like 'replace_*' -or $env:POS_BOOTSTRAP_ADMIN_PASSWORD -like 'replace_*') {
  throw 'Replace the example passwords in .env before starting the application.'
}
