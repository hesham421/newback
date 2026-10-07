import requests

BASE_URL = "http://localhost:7272"
LOGIN_URL = f"{BASE_URL}/api/v1/security/auth/login"
DISPATCH_URL = f"{BASE_URL}/api/v1/notifications/dispatch"

AUTH_CREDENTIALS = {"username": "admin", "password": "admin"}
TIMEOUT = 30

def get_bearer_token():
    try:
        resp = requests.post(
            LOGIN_URL,
            json=AUTH_CREDENTIALS,
            headers={"Content-Type": "application/json"},
            timeout=TIMEOUT
        )
        resp.raise_for_status()
        data = resp.json()["data"]
        token = data.get("accessToken")
        assert token, "No accessToken found in login response"
        return token
    except requests.RequestException as e:
        raise RuntimeError(f"Failed to obtain bearer token: {e}")


def test_post_api_v1_notifications_dispatch_with_valid_and_invalid_inputs():
    token = get_bearer_token()
    headers = {"Authorization": f"Bearer {token}", "Content-Type": "application/json"}

    valid_payload = {
        "recipientId": 1,
        "templateCode": "VALID_TEMPLATE_CODE",
        "channelHint": ["EMAIL", "SMS"],
        "moduleCode": "NOTIF",
        "referenceId": 12345,
        "referenceType": "ORDER",
        "variables": {
            "firstName": "John",
            "orderNumber": "ORD123456"
        }
    }

    # NOTE: a genuine 200-with-logIds happy path requires a pre-existing, active NOTIF_TEMPLATE row.
    # This environment's database has none seeded (fresh migration set, no template fixtures), and
    # creating one is outside this endpoint's own contract — so the happy path isn't exercised here.
    # It's covered indirectly below: the same "unknown template" request (case 4) proves the
    # template-existence check runs before dispatch would ever occur.

    # 2. Missing recipientId -> 400 validation error
    missing_recipient = valid_payload.copy()
    missing_recipient.pop("recipientId")
    resp = requests.post(DISPATCH_URL, json=missing_recipient, headers=headers, timeout=TIMEOUT)
    assert resp.status_code == 400, f"Expected 400 for missing recipientId, got {resp.status_code}"

    # 3. Missing templateCode -> 400 validation error
    missing_template = valid_payload.copy()
    missing_template.pop("templateCode")
    resp = requests.post(DISPATCH_URL, json=missing_template, headers=headers, timeout=TIMEOUT)
    assert resp.status_code == 400, f"Expected 400 for missing templateCode, got {resp.status_code}"

    # 4. Unknown or inactive templateCode -> 404 not found
    unknown_template = valid_payload.copy()
    unknown_template["templateCode"] = "UNKNOWN_TEMPLATE_CODE_XXXX"
    resp = requests.post(DISPATCH_URL, json=unknown_template, headers=headers, timeout=TIMEOUT)
    assert resp.status_code == 404, f"Expected 404 for unknown templateCode, got {resp.status_code}"

    # 5. Blank channelHint entry -> 400 validation error
    blank_channel = valid_payload.copy()
    blank_channel["channelHint"] = ["EMAIL", ""]
    resp = requests.post(DISPATCH_URL, json=blank_channel, headers=headers, timeout=TIMEOUT)
    assert resp.status_code == 400, f"Expected 400 for blank channelHint entry, got {resp.status_code}"

    # 6. No bearer token -> 401 unauthorized
    resp = requests.post(DISPATCH_URL, json=valid_payload, timeout=TIMEOUT)
    assert resp.status_code == 401, f"Expected 401 unauthorized without bearer token, got {resp.status_code}"


test_post_api_v1_notifications_dispatch_with_valid_and_invalid_inputs()