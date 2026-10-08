#!/usr/bin/env bash
# Curl replay of the same demonstration flow as server/tests/e2e/test_study_journey.py.
# Run ONLY against the disposable local lab2 stand. Recorded HTTP includes test credentials.
set -Eeuo pipefail
BASE="${LAB2_API_URL:-http://api:8000}"
SUFFIX="$(date +%s)-$$"
EMAIL="capture-${SUFFIX}@example.com"
PASSWORD="Lab2Capture123!"
TOPIC="capture-topic-${SUFFIX}"
TASK="capture-task-${SUFFIX}"
SOLUTION="capture-solution-${SUFFIX}"
NOW="$(date -u +%Y-%m-%dT%H:%M:%SZ)"
LATER="$(date -u -d '+30 seconds' +%Y-%m-%dT%H:%M:%SZ)"
LATEST="$(date -u -d '+60 seconds' +%Y-%m-%dT%H:%M:%SZ)"
request() { curl --fail-with-body --silent --show-error "$@"; }
json_post() { request -X POST "$BASE$1" -H 'Content-Type: application/json' -d "$2"; }
sync_post() { request -X POST "$BASE/api/v1/sync/push" -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' -d "$1"; }
echo '1. POST /auth/register'
CREDS="$(jq -n --arg email "$EMAIL" --arg password "$PASSWORD" '{email:$email,password:$password}')"
json_post /api/v1/auth/register "$CREDS" | jq '{id,email}'
echo '2. POST /auth/login'
LOGIN="$(json_post /api/v1/auth/login "$CREDS")"
TOKEN="$(jq -r '.accessToken' <<<"$LOGIN")"
REFRESH="$(jq -r '.refreshToken' <<<"$LOGIN")"
echo '3. GET /auth/me'
request "$BASE/api/v1/auth/me" -H "Authorization: Bearer $TOKEN" | jq '{id,email}'
echo '4. POST /sync/initial (topic + task + solution)'
PAYLOAD="$(jq -n --arg t "$TOPIC" --arg task "$TASK" --arg solution "$SOLUTION" --arg now "$NOW" \
  '{push:{topics:[{clientId:$t,title:"StudyMate curl E2E",operation:"CREATE",updatedAt:$now}],tasks:[{clientId:$task,topicClientId:$t,title:"Solve exercise",status:"PLANNED",operation:"CREATE",updatedAt:$now}],solutions:[{clientId:$solution,taskClientId:$task,content:"First attempt",operation:"CREATE",updatedAt:$now}]}}')"
request -X POST "$BASE/api/v1/sync/initial" -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' -d "$PAYLOAD" | jq '{push:.push}'
echo '5. GET /sync/pull'
request -G "$BASE/api/v1/sync/pull" -H "Authorization: Bearer $TOKEN" | jq --arg t "$TOPIC" --arg task "$TASK" '{topic:[.topics[]|select(.clientId==$t)],task:[.tasks[]|select(.clientId==$task)]}'
echo '6. POST /sync/push (update task)'
UPD="$(jq -n --arg t "$TOPIC" --arg task "$TASK" --arg at "$LATER" \
  '{tasks:[{clientId:$task,topicClientId:$t,title:"Solved exercise",status:"DONE",description:"Reviewed",operation:"UPDATE",updatedAt:$at}]}')"
sync_post "$UPD" | jq '{tasks}'
echo '7. POST /sync/push (delete topic and cascade)'
DEL="$(jq -n --arg t "$TOPIC" --arg at "$LATEST" '{topics:[{clientId:$t,operation:"DELETE",updatedAt:$at}]}')"
sync_post "$DEL" | jq '{topics}'
echo '8. GET /sync/pull (tombstones)'
request -G "$BASE/api/v1/sync/pull" -H "Authorization: Bearer $TOKEN" | jq --arg t "$TOPIC" --arg task "$TASK" '{topic:[.topics[]|select(.clientId==$t)],task:[.tasks[]|select(.clientId==$task)]}'
echo '9. POST /auth/refresh'
ROTATED="$(json_post /api/v1/auth/refresh "$(jq -n --arg token "$REFRESH" '{refreshToken:$token}')")"
REFRESH="$(jq -r '.refreshToken' <<<"$ROTATED")"
echo '10. POST /auth/logout'
json_post /api/v1/auth/logout "$(jq -n --arg token "$REFRESH" '{refreshToken:$token}')" | jq .
echo 'Curl HTTP scenario completed.'
