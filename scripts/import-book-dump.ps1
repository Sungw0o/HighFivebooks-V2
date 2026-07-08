param(
    [string]$DumpPath = "",
    [string]$MysqlContainer = "highfive-mysql",
    [string]$Database = "highfive_book",
    [string]$User = "book_user",
    [string]$Password = "book_pass"
)

$ErrorActionPreference = "Stop"

$repoRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
$candidateDumpPaths = @(
    (Join-Path $repoRoot "dumps\highfive_book.sql.gz"),
    (Join-Path $env:USERPROFILE "Desktop\Highfivebooks-dumps\highfive_book_20260708_131615.sql.gz")
)

if ([string]::IsNullOrWhiteSpace($DumpPath)) {
    $DumpPath = $candidateDumpPaths | Where-Object { Test-Path $_ } | Select-Object -First 1
}

if (!(Test-Path $DumpPath)) {
    throw @"
Dump file not found.

Pass an explicit dump path:
  powershell -ExecutionPolicy Bypass -File .\scripts\import-book-dump.ps1 -DumpPath C:\path\to\highfive_book.sql.gz

Or place the dump at:
  $repoRoot\dumps\highfive_book.sql.gz
"@
}

docker inspect $MysqlContainer | Out-Null

$remoteDump = "/tmp/highfive_book.sql.gz"
docker cp $DumpPath "${MysqlContainer}:$remoteDump"

docker exec $MysqlContainer sh -lc "gzip -dc $remoteDump | mysql -u$User -p'$Password' $Database"

docker exec $MysqlContainer sh -lc "mysql -u$User -p'$Password' $Database -e 'SELECT COUNT(*) AS book_count FROM book;'"
