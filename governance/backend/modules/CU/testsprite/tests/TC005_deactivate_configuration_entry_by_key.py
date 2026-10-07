import requests
import uuid

BASE_URL = "http://localhost:7272/actuator/health"
LOGIN_URL = "http://localhost:7272/api/v1/security/auth/login"
CONFIG_URL = "http://localhost:7272/api/v1/common/configurations"

def authenticate():
    login_payload = {"username": "admin", "password": "admin"}
    response = requests.post(LOGIN_URL, json=login_payload, timeout=30)
    response.raise_for_status()
    resp_json = response.json()
    access_token = resp_json["data"]["accessToken"]
    return access_token

def create_configuration_entry(token, config_key, config_value="test-value", notes=None):
    url = f"{CONFIG_URL}"
    payload = {
        "configKey": config_key,
        "configValue": config_value,
    }
    if notes is not None:
        payload["notes"] = notes
    headers = {"Authorization": f"Bearer {token}"}
    response = requests.post(url, json=payload, headers=headers, timeout=30)
    return response

def delete_configuration_entry(token, config_key):
    url = f"{CONFIG_URL}/{config_key}"
    headers = {"Authorization": f"Bearer {token}"}
    response = requests.delete(url, headers=headers, timeout=30)
    return response

def deactivate_configuration_entry_by_key_test():
    token = authenticate()
    headers = {"Authorization": f"Bearer {token}"}

    # Use a unique configKey to ensure clean test
    config_key = f"test_deactivate_{uuid.uuid4()}"

    # Create a new configuration entry to deactivate
    create_resp = create_configuration_entry(token, config_key, config_value="to-be-deactivated")
    assert create_resp.status_code == 200
    create_resp_json = create_resp.json()
    assert create_resp_json["success"] is True
    data = create_resp_json["data"]
    assert data["configKey"] == config_key

    try:
        # Deactivate the existing configKey: expect 204 No Content
        delete_resp = delete_configuration_entry(token, config_key)
        assert delete_resp.status_code == 204

        # Attempt to deactivate a non-existent configKey: expect 404 Not Found
        non_existent_key = f"nonexistent_{uuid.uuid4()}"
        delete_resp_404 = delete_configuration_entry(token, non_existent_key)
        assert delete_resp_404.status_code == 404

    finally:
        # Cleanup: attempt to delete the config entry if it still exists (ignore errors)
        _ = delete_configuration_entry(token, config_key)

deactivate_configuration_entry_by_key_test()