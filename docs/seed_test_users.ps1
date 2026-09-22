# Nexus Test User Seeding Script
# This script registers 3 standard test accounts against a running local backend.
# Ensure the backend is running at http://localhost:5263 before executing.

$baseUrl = "http://localhost:5263/api/auth/register"

$users = @(
    @{
        email = "admin@nexus-app.com"
        password = "nexusAdmin123"
        displayName = "Nexus Admin"
    },
    @{
        email = "jane.doe@nexus-app.com"
        password = "janeDoe789"
        displayName = "Jane Doe"
    },
    @{
        email = "test.user@nexus-app.com"
        password = "testUser456"
        displayName = "Test User"
    }
)

foreach ($user in $users) {
    Write-Host "Registering user: $($user.displayName) ($($user.email))..." -ForegroundColor Cyan
    try {
        $json = $user | ConvertTo-Json
        $response = Invoke-RestMethod -Uri $baseUrl -Method Post -Body $json -ContentType "application/json"
        Write-Host "Success! Account created." -ForegroundColor Green
    } catch {
        $msg = $_.Exception.Message
        if ($_.Exception.InnerException) { $msg += " - " + $_.Exception.InnerException.Message }
        Write-Host "Failed to register $($user.email): $msg" -ForegroundColor Red
        Write-Host "Note: If the user already exists, the backend may return a 400 or 409 conflict." -ForegroundColor Yellow
    }
    Write-Host "-----------------------------------"
}
