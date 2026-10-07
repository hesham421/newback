import requests

BASE_URL = "http://localhost:7272"
LOGIN_PATH = "/api/v1/sec/auth/login"
TIMEOUT = 30

def test_post_api_v1_sec_auth_login_with_valid_credentials():
    url = f"{BASE_URL}{LOGIN_PATH}"
    payload = {
        "username": "admin",
        "password": "admin"
    }
    headers = {
        "Content-Type": "application/json"
    }
    try:
        response = requests.post(url, json=payload, headers=headers, timeout=TIMEOUT)
    except requests.RequestException as e:
        assert False, f"Request failed: {e}"

    assert response.status_code == 200, f"Expected HTTP 200 but got {response.status_code}"
    try:
        json_body = response.json()
    except ValueError:
        assert False, "Response is not valid JSON"

    # Validate response structure according to API spec: {"success": true, "data": ..., "timestamp": ...}
    assert isinstance(json_body, dict), "Response JSON root is not a dict"
    assert json_body.get("success") is True, f"Expected success: true but got {json_body.get('success')}"
    data = json_body.get("data")
    assert data and isinstance(data, dict), "Response 'data' field is missing or not a dict"

    access_token = data.get("accessToken")
    token_type = data.get("tokenType")
    expires_in = data.get("expiresIn")

    assert isinstance(access_token, str) and access_token.strip() != "", "accessToken missing or empty"
    assert isinstance(token_type, str) and token_type.strip() != "", "tokenType missing or empty"
    assert isinstance(expires_in, int) and expires_in > 0, "expiresIn missing or not a positive integer"

test_post_api_v1_sec_auth_login_with_valid_credentials()