<#
Creates the Google Play *upload* key for Nexus and the git-ignored keystore.properties that
app/build.gradle.kts reads for release signing.

  powershell -ExecutionPolicy Bypass -File scripts\create-upload-keystore.ps1

- The keystore is written OUTSIDE the repository: %USERPROFILE%\.nexus\nexus-upload.jks
- A random 32-character password is generated and stored only in keystore.properties (git-ignored).
- Back up BOTH files somewhere safe (e.g. a password manager). With Play App Signing, a lost upload
  key can be reset through Play Console support, but releases are blocked until then.
- Refuses to overwrite an existing keystore.
#>
$ErrorActionPreference = "Stop"
$repo = Split-Path -Parent $PSScriptRoot
$dir = Join-Path $env:USERPROFILE ".nexus"
$store = Join-Path $dir "nexus-upload.jks"
$props = Join-Path $repo "keystore.properties"

if (Test-Path $store) { throw "Keystore already exists at $store. Delete it deliberately if you really want a new key." }

$keytool = Get-Command keytool -ErrorAction SilentlyContinue
if (-not $keytool) {
    $jdk = Get-ChildItem "$env:USERPROFILE\.jdks" -Directory -ErrorAction SilentlyContinue | Sort-Object Name -Descending | Select-Object -First 1
    if (-not $jdk) { throw "keytool not found: install a JDK 17+ or add it to PATH." }
    $keytool = Join-Path $jdk.FullName "bin\keytool.exe"
} else { $keytool = $keytool.Source }

New-Item -ItemType Directory -Force $dir | Out-Null
$chars = [char[]]"ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789"
$bytes = New-Object byte[] 32
[Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($bytes)
$password = -join ($bytes | ForEach-Object { $chars[$_ % $chars.Length] })

& $keytool -genkeypair -v -keystore $store -storetype PKCS12 -alias nexus-upload -keyalg RSA -keysize 4096 `
    -validity 10000 -storepass $password -keypass $password -dname "CN=Nexus, OU=OPSC6312, O=Nexus, C=ZA" | Out-Null

@"
# Release signing for Nexus (git-ignored). Back this file and the keystore up together.
storeFile=$($store -replace '\\', '/')
storePassword=$password
keyAlias=nexus-upload
keyPassword=$password
"@ | Set-Content -Encoding ascii $props

& $keytool -list -v -keystore $store -storepass $password | Select-String "SHA1:|SHA256:|Alias name"
Write-Host "Created $store and $props. Back both up now."
Write-Host "Add the SHA-1 above to your Android OAuth client in Google Cloud Console for Google sign-in on release builds."
