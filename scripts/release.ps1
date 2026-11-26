#!/usr/bin/env pwsh
<#
.SYNOPSIS
    Runs a full Maven Central release (prepare + perform) in one command.

.DESCRIPTION
    Checks the prerequisites, then runs:
        .\mvnw.cmd release:prepare release:perform

    The Maven Release Plugin commits, tags and pushes automatically.
    The GPG passphrase is read from MAVEN_GPG_PASSPHRASE (prompted once for the
    session if the variable is not set).

.EXAMPLE
    .\scripts\release.ps1
#>
$ErrorActionPreference = 'Stop'
Set-Location (Join-Path $PSScriptRoot '..')

Write-Host "== Release checks ==" -ForegroundColor Cyan

# 1. Clean working tree
$status = git status --porcelain
if ($LASTEXITCODE -ne 0) {
    throw "Unable to read the Git status. Is this a Git repository?"
}
if ($status) {
    Write-Host $status
    throw "The working tree is not clean. Commit or stash your changes before releasing."
}

# 2. Branch tracks a remote (required because pushChanges=true)
git rev-parse --abbrev-ref --symbolic-full-name '@{u}' *> $null
if ($LASTEXITCODE -ne 0) {
    throw "The current branch has no upstream. Run: git push -u origin <branch>"
}

# 3. Docker (Testcontainers)
docker info *> $null
if ($LASTEXITCODE -ne 0) {
    throw "Docker is not available. Start Docker Desktop: the tests use Testcontainers."
}

# 4. GPG passphrase
if (-not $env:MAVEN_GPG_PASSPHRASE) {
    $secure = Read-Host "GPG passphrase (MAVEN_GPG_PASSPHRASE is not set)" -AsSecureString
    $bstr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure)
    try {
        $env:MAVEN_GPG_PASSPHRASE = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($bstr)
    }
    finally {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($bstr)
    }
}

# 5. Remove a previous release checkout (read-only Git objects block `mvn clean`)
if (Test-Path target) {
    cmd /c "rmdir /s /q target"
}

Write-Host "== release:prepare release:perform ==" -ForegroundColor Cyan
& .\mvnw.cmd -B release:prepare release:perform
exit $LASTEXITCODE
