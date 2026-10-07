import requests
import uuid

BASE_URL = "http://localhost:7272"
TIMEOUT = 30

def test_post_api_v1_common_configurations_with_valid_data():
    session = requests.Session()
    try:
        # Step 1: Authenticate and get JWT token
        auth_url = f"{BASE_URL}/api/v1/security/auth/login"
        auth_payload = {
            "username": "admin",
            "password": "admin"
        }
        auth_resp = session.post(auth_url, json=auth_payload, timeout=TIMEOUT)
        assert auth_resp.status_code == 200, f"Authentication failed: {auth_resp.text}"
        auth_data = auth_resp.json()
        assert "data" in auth_data and "accessToken" in auth_data["data"], \
            f"Missing accessToken in auth response: {auth_data}"
        access_token = auth_data["data"]["accessToken"]

        # Prepare headers for common configurations endpoint
        headers = {
            "Authorization": f"Bearer {access_token}",
            "Content-Type": "application/json"
        }

        # Generate a unique configKey to prevent duplicate-key conflicts
        unique_key = f"TEST_CONFIG_KEY_{uuid.uuid4().hex[:8].upper()}"
        config_value = "TestConfigValue123"

        # Step 2: POST to create a new configuration entry
        post_url = f"{BASE_URL}/api/v1/common/configurations"
        post_payload = {
            "configKey": unique_key,
            "configValue": config_value
        }

        post_resp = session.post(post_url, json=post_payload, headers=headers, timeout=TIMEOUT)
        assert post_resp.status_code == 201, f"POST common configuration failed: {post_resp.text}"

        post_data = post_resp.json()
        assert "data" in post_data, f"Missing data in POST response: {post_data}"
        created_config = post_data["data"]
        # Validate returned configuration details
        assert created_config.get("configKey") == unique_key, "configKey mismatch in response"
        assert created_config.get("configValue") == config_value, "configValue mismatch in response"

    finally:
        # Cleanup: delete the created configuration to avoid test pollution
        delete_url = f"{BASE_URL}/api/v1/common/configurations/{unique_key}"
        delete_headers = {
            "Authorization": f"Bearer {access_token}"
        }
        try:
            delete_resp = session.delete(delete_url, headers=delete_headers, timeout=TIMEOUT)
            assert delete_resp.status_code == 204, f"Failed to delete test configuration: {delete_resp.text}"
        except Exception:
            # Swallow any exception on cleanup to not mask test results
            pass

test_post_api_v1_common_configurations_with_valid_data()