import requests
import uuid

BASE_URL = "http://localhost:7272"
AUTH_URL = f"{BASE_URL}/api/v1/security/auth/login"
CREATE_CONFIG_URL = f"{BASE_URL}/api/v1/common/configurations"
CONFIG_URL_TEMPLATE = f"{BASE_URL}/api/v1/common/configurations/{{configKey}}"
TIMEOUT = 30

def authenticate(username: str = "admin", password: str = "admin") -> str:
    try:
        resp = requests.post(
            AUTH_URL,
            json={"username": username, "password": password},
            timeout=TIMEOUT,
        )
        resp.raise_for_status()
        json_data = resp.json()
        assert json_data.get("success") is True, "Authentication failed: success flag false"
        token = json_data["data"]["accessToken"]
        assert token, "Authentication failed: no accessToken found"
        return token
    except Exception as e:
        raise RuntimeError(f"Authentication error: {e}")

def test_create_new_configuration_entry():
    token = authenticate()
    headers = {"Authorization": f"Bearer {token}", "Content-Type": "application/json"}

    # Generate a unique configKey to avoid duplicates
    unique_key = f"TEST_CONFIG_{uuid.uuid4().hex[:8].upper()}"
    payload = {
        "configKey": unique_key,
        "configValue": "smtp.example.com",
        "notes": "Test config entry created by TC001"
    }

    try:
        # Create new configuration entry
        response = requests.post(CREATE_CONFIG_URL, headers=headers, json=payload, timeout=TIMEOUT)
        assert response.status_code == 200, f"Expected HTTP 200, got {response.status_code}"
        resp_json = response.json()
        assert resp_json.get("success") is True, "API response success flag should be True"
        data = resp_json.get("data")
        assert data is not None, "Response data is missing"
        assert data.get("configKey") == unique_key, "Returned configKey does not match"
        assert data.get("configValue") == payload["configValue"], "Returned configValue does not match"
        assert data.get("notes") == payload["notes"], "Returned notes does not match"
    finally:
        # Cleanup: delete the created config entry to avoid residue
        delete_url = CONFIG_URL_TEMPLATE.format(configKey=unique_key)
        del_resp = requests.delete(delete_url, headers=headers, timeout=TIMEOUT)
        # Accept 204 No Content or 404 Not Found if deletion already happened
        assert del_resp.status_code in (204, 404), f"Cleanup failed, unexpected status {del_resp.status_code}"

test_create_new_configuration_entry()