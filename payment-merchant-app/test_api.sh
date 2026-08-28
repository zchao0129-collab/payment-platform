#!/bin/bash
# API Integration Test Script
BASE="https://qrpay.csmmkj.cn/api"

# Helper: extract JSON string value for a given key
json_val() {
  echo "$1" | grep -o "\"$2\":\"[^\"]*\"" | head -1 | sed "s/\"$2\":\"//g" | sed 's/"//g'
}

echo "============================================="
echo "  API Integration Test Report"
echo "  Target: $BASE"
echo "============================================="

# Step 1: Captcha ticket
echo ""
echo "[1/7] Captcha Ticket..."
TICKET_RESP=$(curl -s "$BASE/auth/captcha/ticket?scene=1")
TICKET=$(echo "$TICKET_RESP" | grep -o '"data":"[^"]*"' | sed 's/"data":"//' | sed 's/"//')
if [ -z "$TICKET" ]; then
  echo "  FAIL: $TICKET_RESP"
  exit 1
fi
echo "  OK: ticket=${TICKET:0:16}..."

# Step 2: Login
echo ""
echo "[2/7] Login..."
LOGIN_RESP=$(curl -s -X POST "$BASE/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"phone\":\"13800020001\",\"password\":\"123456\",\"captchaTicket\":\"$TICKET\"}")
TOKEN=$(echo "$LOGIN_RESP" | grep -o '"accessToken":"[^"]*"' | sed 's/"accessToken":"//' | sed 's/"//')
if [ -z "$TOKEN" ]; then
  echo "  FAIL: $LOGIN_RESP"
  exit 1
fi
USERNAME=$(echo "$LOGIN_RESP" | grep -o '"username":"[^"]*"' | sed 's/"username":"//' | sed 's/"//')
MERCHANT=$(echo "$LOGIN_RESP" | grep -o '"merchantName":"[^"]*"' | sed 's/"merchantName":"//' | sed 's/"//')
echo "  OK: user=$USERNAME, merchant=$MERCHANT"

# Helper
check() {
  local label="$1" url="$2"
  echo ""
  echo "[3+$((TEST_NUM))/7] $label"
  echo "  GET $url"
  FULL=$(curl -s -w "\n%{http_code}" -H "Authorization: Bearer $TOKEN" "$url")
  HTTP=$(echo "$FULL" | tail -1)
  BODY=$(echo "$FULL" | head -n -1)
  CODE=$(echo "$BODY" | grep -o '"code":[0-9]*' | head -1 | sed 's/"code"://')
  if [ "$HTTP" = "200" ] && [ "$CODE" = "200" ]; then
    echo "  PASS (HTTP $HTTP)"
    echo "$BODY" | head -c 250
    echo ""
  else
    echo "  FAIL (HTTP $HTTP, code=$CODE)"
    echo "$BODY" | head -c 300
  fi
}

TEST_NUM=0

# Step 3: Stats
check "Revenue Stats" "$BASE/statistics/revenue"; TEST_NUM=$((TEST_NUM+1))

# Step 4: Profile
check "Merchant Profile" "$BASE/merchant/profile"; TEST_NUM=$((TEST_NUM+1))

# Step 5: Order List
check "Order List" "$BASE/order/list?page=1&size=2"; TEST_NUM=$((TEST_NUM+1))

# Step 6: Commission Summary
check "Commission Summary" "$BASE/commission/summary"; TEST_NUM=$((TEST_NUM+1))

# Step 7: Commission List
check "Commission List" "$BASE/commission/list?page=1&size=2"; TEST_NUM=$((TEST_NUM+1))

# Step 8: QR Code
check "My QR Code" "$BASE/qrcode/my"; TEST_NUM=$((TEST_NUM+1))

echo ""
echo "============================================="
echo "  ALL TESTS COMPLETE"
echo "============================================="
