$ErrorActionPreference = 'Stop'

function Get-LocalSetting([string]$Name) {
    $line = Get-Content (Join-Path $PSScriptRoot '..\.env.local') |
        Where-Object { $_ -match "^$([regex]::Escape($Name))=" } |
        Select-Object -First 1
    if (-not $line) { throw "Missing $Name in .env.local" }
    return $line.Substring($line.IndexOf('=') + 1)
}

function Invoke-Api([string]$Method, [string]$Path, [string]$Token, $Body = $null) {
    $parameters = @{
        Method = $Method
        Uri = "$script:ApiBase$Path"
        Headers = @{ Authorization = "Bearer $Token" }
    }
    if ($null -ne $Body) {
        $parameters.ContentType = 'application/json'
        $parameters.Body = $Body | ConvertTo-Json -Depth 8
    }
    return Invoke-RestMethod @parameters
}

function New-SmokeUser([string]$AdminToken, [string]$Username, [string]$Password) {
    $body = @{
        username = $Username
        email = "$Username@local.test"
        emailVerified = $true
        firstName = 'Local'
        lastName = 'Smoke'
        requiredActions = @()
        enabled = $true
        credentials = @(@{ type = 'password'; value = $Password; temporary = $false })
    } | ConvertTo-Json -Depth 8
    Invoke-RestMethod -Method Post -Uri "$script:KeycloakBase/admin/realms/journal/users" `
        -Headers @{ Authorization = "Bearer $AdminToken" } -ContentType 'application/json' -Body $body | Out-Null
}

function Get-UserToken([string]$Username, [string]$Password) {
    return (Invoke-RestMethod -Method Post -Uri "$script:KeycloakBase/realms/journal/protocol/openid-connect/token" -Body @{
        client_id = 'journal-web'
        grant_type = 'password'
        username = $Username
        password = $Password
    }).access_token
}

$script:KeycloakBase = 'http://localhost:8180'
$script:ApiBase = "http://localhost:$(Get-LocalSetting 'BACKEND_HOST_PORT')"
$adminUsername = Get-LocalSetting 'KEYCLOAK_ADMIN'
$adminPassword = Get-LocalSetting 'KEYCLOAK_ADMIN_PASSWORD'
$suffix = [guid]::NewGuid().ToString('N').Substring(0, 10)
$firstUsername = "smoke-a-$suffix"
$secondUsername = "smoke-b-$suffix"
$firstPassword = "Smoke-A9-$([guid]::NewGuid().ToString('N'))"
$secondPassword = "Smoke-B8-$([guid]::NewGuid().ToString('N'))"
$client = $null
$adminHeaders = $null

try {
    $adminToken = (Invoke-RestMethod -Method Post -Uri "$script:KeycloakBase/realms/master/protocol/openid-connect/token" -Body @{
        client_id = 'admin-cli'
        grant_type = 'password'
        username = $adminUsername
        password = $adminPassword
    }).access_token
    $adminHeaders = @{ Authorization = "Bearer $adminToken" }
    $client = @(Invoke-RestMethod -Method Get -Uri "$script:KeycloakBase/admin/realms/journal/clients?clientId=journal-web" -Headers $adminHeaders)[0]
    if (-not $client) { throw 'journal-web Keycloak client was not found' }

    $client | Add-Member -MemberType NoteProperty -Name redirectUris -Value @('http://localhost:13000/*', 'http://localhost:3000/*', 'http://localhost:5173/*') -Force
    $client | Add-Member -MemberType NoteProperty -Name webOrigins -Value @('http://localhost:13000', 'http://localhost:3000', 'http://localhost:5173') -Force
    $client | Add-Member -MemberType NoteProperty -Name directAccessGrantsEnabled -Value $true -Force
    Invoke-RestMethod -Method Put -Uri "$script:KeycloakBase/admin/realms/journal/clients/$($client.id)" `
        -Headers $adminHeaders -ContentType 'application/json' -Body ($client | ConvertTo-Json -Depth 30) | Out-Null

    New-SmokeUser $adminToken $firstUsername $firstPassword
    New-SmokeUser $adminToken $secondUsername $secondPassword
    $firstToken = Get-UserToken "$firstUsername@local.test" $firstPassword
    $secondToken = Get-UserToken "$secondUsername@local.test" $secondPassword

    $emptyEntries = @((Invoke-Api 'Get' '/api/journal/entries' $firstToken) | Where-Object { $null -ne $_ })
    if ($emptyEntries.Count -ne 0) { throw 'Disposable first user unexpectedly has journal data' }

    try {
        Invoke-Api 'Post' '/api/journal/entry' $firstToken @{ content = '   ' } | Out-Null
        throw 'Blank journal entry was accepted'
    } catch {
        if ($_.Exception.Response.StatusCode.value__ -ne 400) { throw }
    }

    $created = Invoke-Api 'Post' '/api/journal/entry' $firstToken @{
        content = 'Today I completed my architecture review. Tomorrow at 9 AM I will write the security test report.'
    }
    if (-not $created.id -or $created.processingStatus -ne 'PENDING') { throw 'Journal was not accepted for background processing' }

    $processed = $null
    for ($attempt = 0; $attempt -lt 30 -and $null -eq $processed; $attempt++) {
        Start-Sleep -Seconds 2
        $processed = @((Invoke-Api 'Get' '/api/journal/entries' $firstToken) | Where-Object { $_.id -eq $created.id -and $_.processingStatus -eq 'COMPLETED' }) | Select-Object -First 1
    }
    if ($null -eq $processed -or -not $processed.aiResponse) { throw 'Background reflection did not complete within 60 seconds' }
    $savedEntries = @((Invoke-Api 'Get' '/api/journal/entries' $firstToken) | Where-Object { $null -ne $_ })
    if ($savedEntries.Count -ne 1 -or $savedEntries[0].id -ne $created.id) { throw 'Owner could not list the saved journal entry' }

    $rag = Invoke-Api 'Post' '/api/chat/rag' $firstToken @{ query = 'What did I complete today?' }
    if (-not $rag.reply -or @($rag.referencedEntries | Where-Object { $null -ne $_ }).Count -lt 1) { throw 'RAG response was not grounded in a memory' }

    $items = @()
    for ($attempt = 0; $attempt -lt 30 -and $items.Count -eq 0; $attempt++) {
        Start-Sleep -Seconds 2
        $items = @((Invoke-Api 'Get' '/api/action-items' $firstToken) | Where-Object { $null -ne $_ })
    }
    if ($items.Count -eq 0) { throw 'Accountability outbox did not produce an action item within 60 seconds' }

    if ($items[0].status -eq 'PROPOSED') {
        Invoke-Api 'Patch' "/api/action-items/$($items[0].id)" $firstToken @{ status = 'PENDING' } | Out-Null
    }
    Invoke-Api 'Patch' "/api/action-items/$($items[0].id)" $firstToken @{ status = 'COMPLETED' } | Out-Null
    $reloadedItems = @((Invoke-Api 'Get' '/api/action-items' $firstToken) | Where-Object { $null -ne $_ })
    $completed = $reloadedItems | Where-Object { $_.id -eq $items[0].id } | Select-Object -First 1
    if (-not $completed -or $completed.status -ne 'COMPLETED') { throw 'Action item status did not update' }

    $secondEntries = @((Invoke-Api 'Get' '/api/journal/entries' $secondToken) | Where-Object { $null -ne $_ })
    if ($secondEntries.Count -ne 0) { throw 'Cross-user journal data was exposed' }
    try {
        Invoke-Api 'Patch' "/api/action-items/$($items[0].id)" $secondToken @{ status = 'PENDING' } | Out-Null
        throw 'Cross-user action-item update was accepted'
    } catch {
        if ($_.Exception.Response.StatusCode.value__ -ne 404) { throw }
    }

    [pscustomobject]@{
        Health = (Invoke-RestMethod "$script:ApiBase/actuator/health").status
        JournalCreated = $created.id
        RagReferences = @($rag.referencedEntries | Where-Object { $null -ne $_ }).Count
        ActionItems = $items.Count
        CrossUserIsolation = 'PASS'
    }
} finally {
    if ($client -and $adminHeaders) {
        $client | Add-Member -MemberType NoteProperty -Name directAccessGrantsEnabled -Value $false -Force
        Invoke-RestMethod -Method Put -Uri "$script:KeycloakBase/admin/realms/journal/clients/$($client.id)" `
            -Headers $adminHeaders -ContentType 'application/json' -Body ($client | ConvertTo-Json -Depth 30) | Out-Null
    }
}
