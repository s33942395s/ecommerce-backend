param(
    [string]$HostName = $env:DB_HOST,
    [string]$Port = $env:DB_PORT,
    [string]$Database = $env:DB_NAME,
    [string]$User = $env:DB_USER,
    [string]$Input = "backup.dump"
)

if ([string]::IsNullOrWhiteSpace($HostName)) { $HostName = "localhost" }
if ([string]::IsNullOrWhiteSpace($Port)) { $Port = "5432" }
if ([string]::IsNullOrWhiteSpace($Database)) { $Database = "ecommerce_db" }
if ([string]::IsNullOrWhiteSpace($User)) { $User = "postgres" }

pg_restore `
    --host $HostName `
    --port $Port `
    --username $User `
    --dbname $Database `
    --clean `
    --if-exists `
    $Input
