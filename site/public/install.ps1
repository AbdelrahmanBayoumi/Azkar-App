param(
    [Alias('Channel')]
    [ValidateSet('prompt', 'stable', 'dev')]
    [string]$AzkarInstallChannel = 'prompt'
)

& {
    $ErrorActionPreference = 'Stop'
    $Channel = $AzkarInstallChannel
    if ($env:PROCESSOR_ARCHITECTURE -ne 'AMD64' -and $env:PROCESSOR_ARCHITEW6432 -ne 'AMD64') {
        throw 'This installer supports x64 Windows.'
    }

    function Select-AzkarInstallChannel {
        param([switch]$DevOnly)
        $prompt = if ($DevOnly) {
            'No Stable release is published. [D] Install Dev or [C] Cancel'
        } else {
            'Install Azkar from [S] Stable or [D] Dev (prerelease), or [C] Cancel'
        }
        while ($true) {
            $choice = Read-Host $prompt
            if (-not $choice -or $choice -eq 'C') { return }
            if ($choice -eq 'D') { return 'dev' }
            if (-not $DevOnly -and $choice -eq 'S') { return 'stable' }
            Write-Host 'Choose one of the listed options.'
        }
    }

    if ($Channel -eq 'prompt') {
        $Channel = Select-AzkarInstallChannel
        if ($Channel -eq 'stable') {
            $latestApi = 'https://api.github.com/repos/jamhour1g/Azkar-App/releases/latest'
            try {
                $null = Invoke-WebRequest -UseBasicParsing -Uri $latestApi
            } catch {
                if ([int]$_.Exception.Response.StatusCode -ne 404) {
                    throw
                }
                $Channel = Select-AzkarInstallChannel -DevOnly
            }
        }
        if (-not $Channel) { return }
    }

    $releases = 'https://github.com/jamhour1g/Azkar-App/releases'
    $download = "$releases/latest/download"
    if ($Channel -eq 'dev') {
        $versionUrl = 'https://jamhour1g.github.io/Azkar-App/dev-version.txt'
        $version = (Invoke-WebRequest -UseBasicParsing -Uri $versionUrl).Content.Trim()
        if ($version -cnotmatch '\A[0-9]+\.[0-9]+\.[0-9]+-beta\.[0-9]+(?:\.[0-9]+)*\z') {
            throw 'Invalid Dev release version.'
        }
        $download = "$releases/download/v$version"
    }
    $temp = New-Item -ItemType Directory -Path (Join-Path ([IO.Path]::GetTempPath()) ([guid]::NewGuid().ToString()))
    try {
        $checksums = Join-Path $temp.FullName 'SHA256SUMS'
        Invoke-WebRequest -UseBasicParsing -Uri "$download/SHA256SUMS" -OutFile $checksums
        $entries = @(Get-Content -LiteralPath $checksums | Where-Object {
            $_ -cmatch '^[a-fA-F0-9]{64}\s+azkar-[0-9][0-9A-Za-z.+-]*-windows-x64-nsis\.exe$'
        })
        if ($entries.Count -ne 1) {
            throw "Expected one x64 Windows installer in the $Channel release."
        }

        $digest, $asset = $entries[0] -split '\s+', 2
        $installer = Join-Path $temp.FullName $asset
        Invoke-WebRequest -UseBasicParsing -Uri "$download/$asset" -OutFile $installer
        if ((Get-FileHash -LiteralPath $installer -Algorithm SHA256).Hash -ne $digest) {
            throw 'The downloaded Azkar installer failed SHA-256 verification.'
        }

        Write-Host 'Opening Azkar Setup...'
        $setup = Start-Process -FilePath $installer -Wait -PassThru
        if ($setup.ExitCode -ne 0) {
            throw "Azkar Setup exited with code $($setup.ExitCode)."
        }
    } catch {
        $cause = $_.Exception.GetBaseException()
        if ($cause -is [System.ComponentModel.Win32Exception] -and $cause.NativeErrorCode -in 225, 226) {
            throw "Windows security software blocked Azkar Setup. Check Windows Security > Protection history and report the detection name and installer version to the Azkar project. ($asset)"
        }
        throw
    } finally {
        Remove-Item -LiteralPath $temp.FullName -Recurse -Force
    }
}
