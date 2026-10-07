import requests
import uuid

BASE_URL = "http://localhost:7272/actuator/health"
LOGIN_URL = "http://localhost:7272/api/v1/security/auth/login"
CONFIG_URL = "http://localhost:7272/api/v1/common/configurations"

def test_update_existing_configuration_entry():
    session = requests.Session()
    timeout = 30

    # Authenticate and get token
    auth_payload = {"username": "admin", "password": "admin"}
    auth_resp = session.post(LOGIN_URL, json=auth_payload, timeout=timeout)
    assert auth_resp.status_code == 200, f"Login failed: {auth_resp.text}"
    auth_json = auth_resp.json()
    access_token = auth_json.get("data", {}).get("accessToken")
    assert access_token, "No accessToken in login response"

    headers = {
        "Authorization": f"Bearer {access_token}",
        "Content-Type": "application/json"
    }

    # Create a new configuration entry to update
    unique_key = f"TEST_CONFIG_{uuid.uuid4()}"
    create_payload = {
        "configKey": unique_key,
        "configValue": "initial_value",
        "notes": "Initial notes"
    }

    try:
        create_resp = session.post(CONFIG_URL, json=create_payload, headers=headers, timeout=timeout)
        assert create_resp.status_code == 200, f"Create config failed: {create_resp.text}"
        create_data = create_resp.json()
        assert create_data.get("success") is True
        created_config = create_data.get("data")
        assert created_config["configKey"] == unique_key
        assert created_config["configValue"] == "initial_value"

        # Update the existing configuration entry by configKey
        update_payload = {
            "configValue": "updated_value",
            "notes": "Updated notes"
        }

        update_resp = session.put(f"{CONFIG_URL}/{unique_key}", json=update_payload, headers=headers, timeout=timeout)
        assert update_resp.status_code == 200, f"Update config failed: {update_resp.text}"
        update_data = update_resp.json()
        assert update_data.get("success") is True
        updated_config = update_data.get("data")
        assert updated_config["configKey"] == unique_key
        assert updated_config["configValue"] == "updated_value"
        assert updated_config["notes"] == "Updated notes"

    finally:
        # Cleanup: delete the created configuration
        delete_resp = session.delete(f"{CONFIG_URL}/{unique_key}", headers=headers, timeout=timeout)
        # Deletion might return 204 or 404 if not found, both acceptable for cleanup
        assert delete_resp.status_code in (204, 404), f"Cleanup delete failed: {delete_resp.text}"


test_update_existing_configuration_entry()