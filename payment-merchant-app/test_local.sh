#!/bin/bash
BASE="http://localhost:8080/api"

json_val() {
  echo "$1" | grep -o "\"$2\":\"[^\"]*\"" | head -1 | sed "s/\"$2\":\"//g" | sed 's/"//g'
}

echo "============================================="
echo "  Local API Integration Test"
echo "  Target: $BASE"
echo "============================================="

# Step 1: Captcha
echo ""
echo "[1/8] Captcha Ticket..."
TICKET_RESP=$(curl -s "$BASE/auth/captcha/ticket?scene=1")
TICKET=$(echo "$TICKET_RESP" | grep -o '"data":"[^"]*"' | sed 's/"data":"//' | sed 's/"//')
if [ -z "$TICKET" ]; then echo "  FAIL"; exit 1; fi
echo "  PASS: ticket=${TICKET:0:16}..."

# Step 2: Login
echo ""
echo "[2/8] Login..."
LOGIN_RESP=$(curl -s -X POST "$BASE/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"phone\":\"13800020001\",\"password\":\"123456\",\"captchaTicket\":\"$TICKET\"}")
TOKEN=$(echo "$LOGIN_RESP" | grep -o '"accessToken":"[^"]*"' | sed 's/"accessToken":"//' | sed 's/"//')
USERNAME=$(echo "$LOGIN_RESP" | grep -o '"username":"[^"]*"' | sed 's/"username":"//' | sed 's/"//')
MERCHANT=$(echo "$LOGIN_RESP" | grep -o '"merchantName":"[^"]*"' | sed 's/"merchantName":"//' | sed 's/"//')
if [ -z "$TOKEN" ]; then echo "  FAIL: $LOGIN_RESP"; exit 1; fi
echo "  PASS: user=$USERNAME, merchant=$MERCHANT"

# Helper
check() {
  local label="$1" url="$2"
  local num="$3"
  echo ""
  echo "[$num/8] $label"
  FULL=$(curl -s -w "\n%{http_code}" -H "Authorization: Bearer $TOKEN" "$url")
  HTTP=$(echo "$FULL" | tail -1)
  BODY=$(echo "$FULL" | head -n -1)
  CODE=$(echo "$BODY" | grep -o '"code":[0-9]*' | head -1 | sed 's/"code"://')
  if [ "$HTTP" = "200" ] && [ "$CODE" = "200" ]; then
    echo "  PASS (HTTP $HTTP)"
    echo "$BODY" | head -c 300
    echo ""
  else
    echo "  FAIL (HTTP $HTTP, code=$CODE)"
    echo "$BODY" | head -c 300
  fi
}

# Step 3-8
check "Revenue Stats"      "$BASE/statistics/revenue"          3
check "Merchant Profile"   "$BASE/merchant/profile"            4
check "Order List"         "$BASE/order/list?page=1&size=2"    5
check "Commission Summary" "$BASE/commission/summary"           6
check "Commission List"    "$BASE/commission/list?page=1&size=2" 7
check "My QR Code"         "$BASE/qrcode/my"                   8

echo ""
echo "============================================="
echo "  LOCAL TESTS COMPLETE"
echo "============================================="
