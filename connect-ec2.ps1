param(
    [string]$HostName = "54.224.68.1",
    [string]$User = "ec2-user",
    [string]$KeyPath = "$env:USERPROFILE\.ssh\highfivebooks.pem"
)

$ErrorActionPreference = "Stop"

if (!(Test-Path $KeyPath)) {
    throw "SSH key not found: $KeyPath"
}

$resolvedKey = (Resolve-Path $KeyPath).Path

# Windows OpenSSH may reject keys if inherited permissions are too broad.
icacls $resolvedKey /inheritance:r | Out-Null
icacls $resolvedKey /grant:r "$env:USERNAME`:R" | Out-Null

Write-Host "Connecting to $User@$HostName with $resolvedKey"
ssh `
    -i $resolvedKey `
    -o IdentitiesOnly=yes `
    -o StrictHostKeyChecking=accept-new `
    -o ServerAliveInterval=30 `
    "$User@$HostName"
