$ErrorActionPreference = "Stop"

$Root = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
Set-Location $Root

$PathInRepo = "tools/MinecraftRemapping"
$Pin = "8ca7ba25dfd67eae43b3c73d02603ff6c085a6d7"

& git rev-parse --is-inside-work-tree *> $null
if ($LASTEXITCODE -ne 0) {
    throw "This command must be run inside the NilLoaderSDK Git repository."
}

# A ZIP cannot store Git's mode-160000 index entry. Recreate the exact pinned
# gitlink from .gitmodules, then initialize its working tree normally.
if (Test-Path $PathInRepo) { Remove-Item -Recurse -Force $PathInRepo }
& git add .gitmodules
& git update-index --add --cacheinfo "160000,$Pin,$PathInRepo"
if ($LASTEXITCODE -ne 0) { throw "Could not stage MinecraftRemapping gitlink." }
& git submodule sync -- $PathInRepo
& git submodule update --init -- $PathInRepo
if ($LASTEXITCODE -ne 0) { throw "Could not initialize MinecraftRemapping submodule." }

$Actual = (& git -C $PathInRepo rev-parse HEAD).Trim()
if ($Actual -ne $Pin) {
    throw "Unexpected MinecraftRemapping commit: $Actual (expected $Pin)"
}

Write-Host "MinecraftRemapping submodule staged at $Pin"
