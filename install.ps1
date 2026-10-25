# Guitar Simulator - Windows installer (PowerShell)
# ---------------------------------------------------
# Downloads guitar-simulator.jar to your Desktop and checks that Java is installed.
#
# One-liner to share with friends (run in PowerShell):
#
#   irm https://raw.githubusercontent.com/YOUR_GH_USER/guitar-simulator/main/install.ps1 | iex
#
# (Replace YOUR_GH_USER with your GitHub username after you've published the repo.)

 $ErrorActionPreference = "Stop"

# ---- CONFIG: edit these two lines after you create the repo on GitHub ----
 $GhUser = "jmducut2014"
 $Repo   = "guitar-simulator"
# --------------------------------------------------------------------------

 $JarName = "guitar-simulator.jar"
 $JarUrl  = "https://github.com/$GhUser/$Repo/releases/latest/download/$JarName"
 $Desktop = [Environment]::GetFolderPath("Desktop")
 $Dest    = Join-Path $Desktop $JarName

# ---- Java check ----
 $java = Get-Command java -ErrorAction SilentlyContinue
if (-not $java) {
    Write-Host ""
    Write-Host "================================================================" -ForegroundColor Red
    Write-Host "  Java is not installed." -ForegroundColor Red
    Write-Host "  Install Java 17+ from:    https://adoptium.net" -ForegroundColor Red
    Write-Host "  Or run:    winget install EclipseAdoptium.Temurin.21.JDK" -ForegroundColor Red
    Write-Host "================================================================" -ForegroundColor Red
    exit 1
}

# Get Java version (works on Temurin / Oracle / OpenJDK)
 $verOut = & java -version 2>&1 | Out-String
if ($verOut -match 'version "(\d+)') {
    $javaMajor = [int]$Matches[1]
    if ($javaMajor -lt 17) {
        Write-Host ""
        Write-Host "================================================================" -ForegroundColor Red
        Write-Host "  Java is too old. Need 17+, you have $javaMajor." -ForegroundColor Red
        Write-Host "  Update from:    https://adoptium.net" -ForegroundColor Red
        Write-Host "================================================================" -ForegroundColor Red
        exit 1
    }
}

# ---- Download ----
Write-Host "Downloading $JarName..." -ForegroundColor Cyan
try {
    Invoke-WebRequest -Uri $JarUrl -OutFile $Dest -UseBasicParsing
} catch {
    Write-Host ""
    Write-Host "================================================================" -ForegroundColor Red
    Write-Host "  Download failed." -ForegroundColor Red
    Write-Host "  Possible causes:"
    Write-Host "    * You haven't published a GitHub Release with the jar attached."
    Write-Host "    * `$GhUser / `$Repo at the top of this script are still the placeholders."
    Write-Host "================================================================" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "  Installed: $Dest" -ForegroundColor Green
Write-Host "  Run it:    java -jar `"$Dest`"" -ForegroundColor Green
Write-Host "            (or just double-click the jar in File Explorer)" -ForegroundColor Green
Write-Host ""
