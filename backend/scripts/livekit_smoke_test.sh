#!/usr/bin/env bash
set -euo pipefail

# Simple smoke test for LiveKit session flow through Mockly backend.
# What it validates:
# 1) register candidate + interviewer
# 2) create session
# 3) interviewer can discover the created session ID
# 4) both users join session
# 5) both users receive LiveKit tokens for the same room
# 6) session becomes ACTIVE
#
# Optional:
# - set AUTO_END=true to end the created session at the end of the script
#
# Environment variables:
# BASE_URL   (default: http://localhost:8080/api)
# PASSWORD   (default: TestPass123!)
# AUTO_END   (default: false)
# KEEP_LOGS  (default: false) if true, writes latest JSON responses to /tmp

BASE_URL="${BASE_URL:-http://localhost:8080/api}"
PASSWORD="${PASSWORD:-TestPass123!}"
AUTO_END="${AUTO_END:-false}"
KEEP_LOGS="${KEEP_LOGS:-false}"
AUTO_END_NORMALIZED="$(echo "${AUTO_END}" | tr '[:upper:]' '[:lower:]')"

HTTP_STATUS=""
HTTP_BODY=""

require_cmd() {
  local cmd="$1"
  if ! command -v "${cmd}" >/dev/null 2>&1; then
    echo "Missing required command: ${cmd}"
    exit 1
  fi
}

require_cmd curl
require_cmd jq

print_json_or_raw() {
  if echo "${HTTP_BODY}" | jq . >/dev/null 2>&1; then
    echo "${HTTP_BODY}" | jq .
  else
    echo "${HTTP_BODY}"
  fi
}

http_call() {
  local method="$1"
  local path="$2"
  local token="${3:-}"
  local data="${4:-}"

  local body_file
  body_file="$(mktemp)"

  local -a curl_args=(
    -sS
    -o "${body_file}"
    -w "%{http_code}"
    -X "${method}"
    "${BASE_URL}${path}"
    -H "Accept: application/json"
  )

  if [[ -n "${token}" ]]; then
    curl_args+=(-H "Authorization: Bearer ${token}")
  fi

  if [[ -n "${data}" ]]; then
    curl_args+=(-H "Content-Type: application/json" --data "${data}")
  fi

  local status
  if ! status="$(curl "${curl_args[@]}")"; then
    echo "HTTP call failed: ${method} ${BASE_URL}${path}"
    if [[ -s "${body_file}" ]]; then
      HTTP_BODY="$(cat "${body_file}")"
      print_json_or_raw
    fi
    rm -f "${body_file}"
    exit 1
  fi

  HTTP_STATUS="${status}"
  HTTP_BODY="$(cat "${body_file}")"
  rm -f "${body_file}"

  if [[ "${KEEP_LOGS}" == "true" ]]; then
    printf "%s\n" "${HTTP_BODY}" > "/tmp/mockly_last_response.json"
  fi
}

expect_status() {
  local expected="$1"
  local step="$2"
  if [[ "${HTTP_STATUS}" != "${expected}" ]]; then
    echo "FAILED: ${step}"
    echo "Expected HTTP ${expected}, got ${HTTP_STATUS}"
    print_json_or_raw
    exit 1
  fi
}

json_required() {
  local query="$1"
  local value
  if ! value="$(jq -er "${query}" <<<"${HTTP_BODY}" 2>/dev/null)"; then
    echo "FAILED: response missing required field ${query}"
    print_json_or_raw
    exit 1
  fi
  echo "${value}"
}

require_non_empty() {
  local name="$1"
  local value="$2"
  if [[ -z "${value}" || "${value}" == "null" ]]; then
    echo "FAILED: ${name} is empty"
    exit 1
  fi
}

echo "Running LiveKit smoke test against: ${BASE_URL}"

suffix="$(date +%s)-$RANDOM"
candidate_email="candidate.${suffix}@example.com"
interviewer_email="interviewer.${suffix}@example.com"
candidate_name="Candidate Smoke ${suffix}"
interviewer_name="Interviewer Smoke ${suffix}"
scheduled_at="$(date -u +"%Y-%m-%dT%H:%M:%SZ")"

echo "1/8 Register candidate"
http_call "POST" "/auth/register" "" \
  "{\"email\":\"${candidate_email}\",\"password\":\"${PASSWORD}\",\"displayName\":\"${candidate_name}\",\"role\":\"CANDIDATE\"}"
expect_status "201" "register candidate"
candidate_access_token="$(json_required '.accessToken')"
candidate_user_id="$(json_required '.userId')"
require_non_empty "candidate_access_token" "${candidate_access_token}"
require_non_empty "candidate_user_id" "${candidate_user_id}"

echo "2/8 Register interviewer"
http_call "POST" "/auth/register" "" \
  "{\"email\":\"${interviewer_email}\",\"password\":\"${PASSWORD}\",\"displayName\":\"${interviewer_name}\",\"role\":\"INTERVIEWER\"}"
expect_status "201" "register interviewer"
interviewer_access_token="$(json_required '.accessToken')"
interviewer_user_id="$(json_required '.userId')"
require_non_empty "interviewer_access_token" "${interviewer_access_token}"
require_non_empty "interviewer_user_id" "${interviewer_user_id}"

echo "3/8 Create session"
http_call "POST" "/sessions" "${candidate_access_token}" \
  "{\"interviewerId\":\"${interviewer_user_id}\",\"scheduledAt\":\"${scheduled_at}\"}"
expect_status "201" "create session"
session_id="$(json_required '.id')"
created_room_id="$(json_required '.roomId')"
require_non_empty "session_id" "${session_id}"
require_non_empty "created_room_id" "${created_room_id}"

expected_room_id="session-${session_id}"
if [[ "${created_room_id}" != "${expected_room_id}" ]]; then
  echo "FAILED: roomId mismatch"
  echo "Expected: ${expected_room_id}"
  echo "Got: ${created_room_id}"
  exit 1
fi

echo "4/8 Interviewer discovers active session"
http_call "GET" "/sessions/me/active" "${interviewer_access_token}" ""
expect_status "200" "interviewer active session lookup"
interviewer_active_session_id="$(json_required '.id')"
if [[ "${interviewer_active_session_id}" != "${session_id}" ]]; then
  echo "FAILED: interviewer active session ID mismatch"
  echo "Expected: ${session_id}"
  echo "Got: ${interviewer_active_session_id}"
  exit 1
fi

echo "5/8 Candidate joins session"
http_call "POST" "/sessions/${session_id}/join" "${candidate_access_token}" ""
expect_status "200" "candidate join"

echo "6/8 Interviewer joins session"
http_call "POST" "/sessions/${session_id}/join" "${interviewer_access_token}" ""
expect_status "200" "interviewer join"

echo "7/8 Get LiveKit token for candidate"
http_call "GET" "/sessions/${session_id}/token" "${candidate_access_token}" ""
expect_status "200" "candidate livekit token"
candidate_lk_token="$(json_required '.token')"
candidate_lk_room_id="$(json_required '.roomId')"
livekit_url="$(json_required '.url')"
require_non_empty "candidate_lk_token" "${candidate_lk_token}"
require_non_empty "candidate_lk_room_id" "${candidate_lk_room_id}"
require_non_empty "livekit_url" "${livekit_url}"

echo "8/8 Get LiveKit token for interviewer"
http_call "GET" "/sessions/${session_id}/token" "${interviewer_access_token}" ""
expect_status "200" "interviewer livekit token"
interviewer_lk_token="$(json_required '.token')"
interviewer_lk_room_id="$(json_required '.roomId')"
require_non_empty "interviewer_lk_token" "${interviewer_lk_token}"
require_non_empty "interviewer_lk_room_id" "${interviewer_lk_room_id}"

if [[ "${candidate_lk_room_id}" != "${interviewer_lk_room_id}" ]]; then
  echo "FAILED: users received different LiveKit room IDs"
  echo "candidate roomId: ${candidate_lk_room_id}"
  echo "interviewer roomId: ${interviewer_lk_room_id}"
  exit 1
fi

http_call "GET" "/sessions/${session_id}" "${candidate_access_token}" ""
expect_status "200" "get session after joins"
session_status="$(json_required '.status')"
if [[ "${session_status}" != "ACTIVE" ]]; then
  echo "FAILED: expected session status ACTIVE after joins, got ${session_status}"
  exit 1
fi

if [[ "${AUTO_END_NORMALIZED}" == "true" ]]; then
  echo "Auto cleanup: ending session"
  http_call "POST" "/sessions/${session_id}/end" "${candidate_access_token}" ""
  expect_status "200" "end session"
fi

echo
echo "Smoke test passed."
echo "Session ID: ${session_id}"
echo "Room ID: ${candidate_lk_room_id}"
echo "LiveKit URL: ${livekit_url}"
echo "Candidate user ID: ${candidate_user_id}"
echo "Interviewer user ID: ${interviewer_user_id}"
echo
echo "Use these tokens in two clients (candidate/interviewer) to verify media stability:"
echo "Candidate token:"
echo "${candidate_lk_token}"
echo
echo "Interviewer token:"
echo "${interviewer_lk_token}"
echo
if [[ "${AUTO_END_NORMALIZED}" != "true" ]]; then
  echo "Manual cleanup command:"
  echo "curl -X POST \"${BASE_URL}/sessions/${session_id}/end\" -H \"Authorization: Bearer ${candidate_access_token}\""
fi
