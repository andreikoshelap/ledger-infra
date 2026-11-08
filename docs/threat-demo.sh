#!/usr/bin/env bash
# Generates attacker-like traffic against a locally running ledger-api so the
# Security page has something to show. Start the backend first:
#
#   cd ledger-api
#   ./gradlew bootRun --args='--spring.profiles.active=demo --ledger.security.trust-forwarded-for=true'
#
# The demo profile seeds user 1 with accounts 1..3 and disables the external
# pre-debit check. Safe to run repeatedly; it only talks to localhost by default.
set -euo pipefail

BASE="${BASE:-http://localhost:8080}"
BAD_IP="203.0.113.66"   # listed in ledger.security.malicious-ips (documentation range)

call() { # user method path [json-body] [source-ip]
  local user="$1" method="$2" path="$3" body="${4:-}" ip="${5:-198.51.100.7}"
  local args=(-s -o /dev/null -w "%{http_code} ${method} ${path} (user ${user})\n" \
    -X "$method" -H "X-User-Id: ${user}" -H "X-Forwarded-For: ${ip}")
  if [[ -n "$body" ]]; then args+=(-H "Content-Type: application/json" -d "$body"); fi
  curl "${args[@]}" "${BASE}${path}"
}

echo "== 1. Attacker (user 7) reads another user's accounts and enumerates ids"
for id in $(seq 1 12); do call 7 GET "/api/accounts/${id}/balance"; done

echo "== 2. Same attacker, known-bad address, deposits into a foreign account"
call 7 POST "/api/accounts/1/deposit" '{"amount":"1.00"}' "$BAD_IP"

echo "== 3. Legitimate owner (user 1) drains an account in a burst of debits"
for _ in $(seq 1 12); do call 1 POST "/api/accounts/1/debit" '{"amount":"0.01"}'; done

echo
echo "Summary:"
curl -s "${BASE}/api/security/summary"; echo
echo "Open http://localhost:4200/security (ng serve) or GET ${BASE}/api/security/findings"
