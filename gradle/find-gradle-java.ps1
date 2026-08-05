$ErrorActionPreference = "SilentlyContinue"

function Get-JavaMajor([string]$Home) {
    if ([string]::IsNullOrWhiteSpace($Home)) { return $null }

    $Java = Join-Path $Home "bin\java.exe"
    if (-not (Test-Path $Java)) { return $null }

    $Text = (& $Java -XshowSettings:properties -version 2>&1 | Out-String)

    if ($Text -match 'java\.specification\.version\s*=\s*([0-9.]+)') {
        $v = $Matches[1]
    } elseif ($Text -match 'version\s+"([0-9.]+)') {
        $v = $Matches[1]
    } else {
        return $null
    }

    if ($v.StartsWith('1.')) {
        return [int](($v -split '\.')[1])
    }

    return [int](($v -split '\.')[0])
}

function Test-SupportedJava([string]$Home) {
    $Major = Get-JavaMajor $Home
    return $Major -in @(17, 18, 19, 20, 21, 22)
}

# Explicit override
if (-not [string]::IsNullOrWhiteSpace($env:NILSDK_GRADLE_JAVA_HOME)) {
    $Home = $env:NILSDK_GRADLE_JAVA_HOME.Trim('"')

    if (Test-SupportedJava $Home) {
        Write-Output $Home
        exit 0
    }

    [Console]::Error.WriteLine(
        "NilLoaderSDK: NILSDK_GRADLE_JAVA_HOME is not a supported JDK (17-22): $Home"
    )
    exit 1
}

# JAVA_HOME, including the value set by actions/setup-java
if (-not [string]::IsNullOrWhiteSpace($env:JAVA_HOME)) {
    $Home = $env:JAVA_HOME.Trim('"')

    if (Test-SupportedJava $Home) {
        Write-Output $Home
        exit 0
    }
}

$Candidates = New-Object System.Collections.Generic.List[string]

function Add-Candidate([string]$Home) {
    if ([string]::IsNullOrWhiteSpace($Home)) { return }

    $Home = $Home.Trim('"')

    if (
        (Test-Path (Join-Path $Home 'bin\java.exe')) -and
        -not $Candidates.Contains($Home)
    ) {
        $Candidates.Add($Home)
    }
}

$PathJava = Get-Command java.exe -ErrorAction SilentlyContinue

if ($PathJava) {
    $Bin = Split-Path -Parent $PathJava.Source
    Add-Candidate (Split-Path -Parent $Bin)
}

$Roots = @(
    "$env:ProgramFiles\Eclipse Adoptium",
    "$env:ProgramFiles\Java",
    "$env:ProgramFiles\Microsoft",
    "$env:ProgramFiles\BellSoft",
    "$env:ProgramFiles\Amazon Corretto",
    "$env:LOCALAPPDATA\Programs\Eclipse Adoptium",
    "$env:USERPROFILE\.jdks"
)

if (${env:ProgramFiles(x86)}) {
    $Roots += "${env:ProgramFiles(x86)}\Java"
}

foreach ($Root in $Roots) {
    if (Test-Path $Root) {
        Get-ChildItem -Path $Root -Directory -ErrorAction SilentlyContinue |
            ForEach-Object {
                Add-Candidate $_.FullName
            }
    }
}

foreach ($Preferred in @(21, 17, 22, 20, 19, 18)) {
    foreach ($Home in $Candidates) {
        if ((Get-JavaMajor $Home) -eq $Preferred) {
            Write-Output $Home
            exit 0
        }
    }
}

[Console]::Error.WriteLine(
    'NilLoaderSDK: no compatible Gradle JVM found.'
)
[Console]::Error.WriteLine(
    'Install JDK 21 or 17, or set NILSDK_GRADLE_JAVA_HOME to its home directory.'
)
[Console]::Error.WriteLine(
    'Gradle 8.8 cannot run on Java 25 (class-file major 69).'
)

exit 1