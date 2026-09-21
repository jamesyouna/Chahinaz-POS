schtasks /Delete /TN 'Chahinaz POS Startup' /F 2>$null
schtasks /Delete /TN 'Chahinaz POS Backup' /F 2>$null
Write-Host 'Chahina''z POS scheduled tasks removed. Data and backups were not deleted.'
