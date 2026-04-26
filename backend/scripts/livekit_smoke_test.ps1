param(
    [string]$BaseUrl = "http://localhost:8080/api",
    [string]$Password = "TestPass123!",
    [string]$AutoEnd = "false",
    [string]$KeepLogs = "false"
)

$ErrorActionPreference = "Stop"

$AutoEndNormalized = $AutoEnd.ToLower()

$script:LastStatusCode = $null
$script:LastBodyText = $null
$script:LastBodyJson = $null

function Require-Command {
    param(
        [Parameter(Mandatory = $true)]
        [string]$CommandName
    )

    if (-not (Get-Command $CommandName -ErrorAction SilentlyContinue)) {
        Write-Host "Missing required command: $CommandName"
        exit 1
    }
}

function Write-JsonOrRaw {
    if ($null -ne $script:LastBodyJson) {
        $script:LastBodyJson | ConvertTo-Json -Depth 20
    } elseif ($null -ne $script:LastBodyText) {
        Write-Host $script:LastBodyText
    }
}

function Invoke-HttpCall {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Method,

        [Parameter(Mandatory = $true)]
        [string]$Path,

        [string]$Token = "",

        [object]$Data = $null
    )

    $url = "$BaseUrl$Path"

    $headers = @{
        Accept = "application/json"
    }

    if (-not [string]::IsNullOrWhiteSpace($Token)) {
        $headers["Authorization"] = "Bearer $Token"
    }

    $bodyJson = $null
    if ($null -ne $Data) {
        $bodyJson = $Data | ConvertTo-Json -Depth 20 -Compress
        $headers["Content-Type"] = "application/json"
    }

    try {
        if ($null -ne $bodyJson) {
            $response = Invoke-WebRequest -UseBasicParsing -Uri $url -Method $Method -Headers $headers -Body $bodyJson
        } else {
            $response = Invoke-WebRequest -UseBasicParsing -Uri $url -Method $Method -Headers $headers
        }

        $script:LastStatusCode = [int]$response.StatusCode
        $script:LastBodyText = $response.Content

        try {
            $script:LastBodyJson = $script:LastBodyText | ConvertFrom-Json
        } catch {
            $script:LastBodyJson = $null
        }
    } catch {
        if ($_.Exception.Response -ne $null) {
            $response = $_.Exception.Response
            $script:LastStatusCode = [int]$response.StatusCode

            $stream = $response.GetResponseStream()
            if ($null -ne $stream) {
                $reader = New-Object System.IO.StreamReader($stream)
                $script:LastBodyText = $reader.ReadToEnd()
                $reader.Close()
            } else {
                $script:LastBodyText = $null
            }

            try {
                $script:LastBodyJson = $script:LastBodyText | ConvertFrom-Json
            } catch {
                $script:LastBodyJson = $null
            }

            Write-Host "HTTP call failed: $Method $url"
            Write-JsonOrRaw
            exit 1
        } else {
            Write-Host "HTTP call failed: $Method $url"
            Write-Host $_.Exception.Message
            exit 1
        }
    }

    if ($KeepLogs -eq "true") {
        $logDir = Join-Path $env:TEMP "mockly"
        if (-not (Test-Path $logDir)) {
            New-Item -ItemType Directory -Path $logDir | Out-Null
        }

        $logFile = Join-Path $logDir "mockly_last_response.json"
        if ($null -ne $script:LastBodyText) {
            Set-Content -Path $logFile -Value $script:LastBodyText -Encoding UTF8
        }
    }
}

function Expect-Status {
    param(
        [Parameter(Mandatory = $true)]
        [int]$Expected,

        [Parameter(Mandatory = $true)]
        [string]$Step
    )

    if ($script:LastStatusCode -ne $Expected) {
        Write-Host "FAILED: $Step"
        Write-Host "Expected HTTP $Expected, got $($script:LastStatusCode)"
        Write-JsonOrRaw
        exit 1
    }
}

function Get-RequiredField {
    param(
        [Parameter(Mandatory = $true)]
        [string]$FieldName
    )

    if ($null -eq $script:LastBodyJson) {
        Write-Host "FAILED: response is not valid JSON"
        Write-JsonOrRaw
        exit 1
    }

    $property = $script:LastBodyJson.PSObject.Properties[$FieldName]
    if ($null -eq $property) {
        Write-Host "FAILED: response missing required field .$FieldName"
        Write-JsonOrRaw
        exit 1
    }

    $value = $property.Value
    if ($null -eq $value -or [string]::IsNullOrWhiteSpace([string]$value)) {
        Write-Host "FAILED: $FieldName is empty"
        exit 1
    }

    return [string]$value
}

Write-Host "Running LiveKit smoke test against: $BaseUrl"

Require-Command -CommandName "curl"

$suffix = "$(Get-Date -Format 'yyyyMMddHHmmss')-$([System.Guid]::NewGuid().ToString('N').Substring(0, 6))"
$candidateEmail = "candidate.$suffix@example.com"
$interviewerEmail = "interviewer.$suffix@example.com"
$candidateName = "Candidate Smoke $suffix"
$interviewerName = "Interviewer Smoke $suffix"
$scheduledAt = (Get-Date).ToUniversalTime().ToString("yyyy-MM-ddTHH:mm:ssZ")

Write-Host "1/7 Register candidate"
Invoke-HttpCall -Method "POST" -Path "/auth/register" -Data @{
    email = $candidateEmail
    password = $Password
    displayName = $candidateName
    role = "CANDIDATE"
}
Expect-Status -Expected 201 -Step "register candidate"
$candidateAccessToken = Get-RequiredField -FieldName "accessToken"
$candidateUserId = Get-RequiredField -FieldName "userId"

Write-Host "2/7 Register interviewer"
Invoke-HttpCall -Method "POST" -Path "/auth/register" -Data @{
    email = $interviewerEmail
    password = $Password
    displayName = $interviewerName
    role = "INTERVIEWER"
}
Expect-Status -Expected 201 -Step "register interviewer"
$interviewerAccessToken = Get-RequiredField -FieldName "accessToken"
$interviewerUserId = Get-RequiredField -FieldName "userId"

Write-Host "3/7 Create session"
Invoke-HttpCall -Method "POST" -Path "/sessions" -Token $candidateAccessToken -Data @{
    interviewerId = $interviewerUserId
    scheduledAt = $scheduledAt
}
Expect-Status -Expected 201 -Step "create session"
$sessionId = Get-RequiredField -FieldName "id"
$createdRoomId = Get-RequiredField -FieldName "roomId"

$expectedRoomId = "session-$sessionId"
if ($createdRoomId -ne $expectedRoomId) {
    Write-Host "FAILED: roomId mismatch"
    Write-Host "Expected: $expectedRoomId"
    Write-Host "Got: $createdRoomId"
    exit 1
}

Write-Host "4/7 Candidate joins session"
Invoke-HttpCall -Method "POST" -Path "/sessions/$sessionId/join" -Token $candidateAccessToken
Expect-Status -Expected 200 -Step "candidate join"

Write-Host "5/7 Interviewer joins session"
Invoke-HttpCall -Method "POST" -Path "/sessions/$sessionId/join" -Token $interviewerAccessToken
Expect-Status -Expected 200 -Step "interviewer join"

Write-Host "6/7 Get LiveKit token for candidate"
Invoke-HttpCall -Method "GET" -Path "/sessions/$sessionId/token" -Token $candidateAccessToken
Expect-Status -Expected 200 -Step "candidate livekit token"
$candidateLkToken = Get-RequiredField -FieldName "token"
$candidateLkRoomId = Get-RequiredField -FieldName "roomId"
$livekitUrl = Get-RequiredField -FieldName "url"

Write-Host "7/7 Get LiveKit token for interviewer"
Invoke-HttpCall -Method "GET" -Path "/sessions/$sessionId/token" -Token $interviewerAccessToken
Expect-Status -Expected 200 -Step "interviewer livekit token"
$interviewerLkToken = Get-RequiredField -FieldName "token"
$interviewerLkRoomId = Get-RequiredField -FieldName "roomId"

if ($candidateLkRoomId -ne $interviewerLkRoomId) {
    Write-Host "FAILED: users received different LiveKit room IDs"
    Write-Host "candidate roomId: $candidateLkRoomId"
    Write-Host "interviewer roomId: $interviewerLkRoomId"
    exit 1
}

Invoke-HttpCall -Method "GET" -Path "/sessions/$sessionId" -Token $candidateAccessToken
Expect-Status -Expected 200 -Step "get session after joins"
$sessionStatus = Get-RequiredField -FieldName "status"

if ($sessionStatus -ne "ACTIVE") {
    Write-Host "FAILED: expected session status ACTIVE after joins, got $sessionStatus"
    exit 1
}

if ($AutoEndNormalized -eq "true") {
    Write-Host "Auto cleanup: ending session"
    Invoke-HttpCall -Method "POST" -Path "/sessions/$sessionId/end" -Token $candidateAccessToken
    Expect-Status -Expected 200 -Step "end session"
}

Write-Host ""
Write-Host "Smoke test passed."
Write-Host "Session ID: $sessionId"
Write-Host "Room ID: $candidateLkRoomId"
Write-Host "LiveKit URL: $livekitUrl"
Write-Host "Candidate user ID: $candidateUserId"
Write-Host "Interviewer user ID: $interviewerUserId"
Write-Host ""
Write-Host "Use these tokens in two clients (candidate/interviewer) to verify media stability:"
Write-Host "Candidate token:"
Write-Host $candidateLkToken
Write-Host ""
Write-Host "Interviewer token:"
Write-Host $interviewerLkToken
Write-Host ""

if ($AutoEndNormalized -ne "true") {
    Write-Host "Manual cleanup command:"
    Write-Host "curl -X POST `"$BaseUrl/sessions/$sessionId/end`" -H `"Authorization: Bearer $candidateAccessToken`""
}
