import requests

BASE_URL = "http://localhost:7272"
LOGIN_URL = f"{BASE_URL}/api/v1/security/auth/login"
CONFIG_URL = f"{BASE_URL}/api/v1/common/configurations"
TIMEOUT = 30


def authenticate():
    creds = {"username": "admin", "password": "admin"}
    resp = requests.post(LOGIN_URL, json=creds, timeout=TIMEOUT)
    resp.raise_for_status()
    json_data = resp.json()
    assert json_data.get("success") is True
    access_token = json_data["data"]["accessToken"]
    return access_token


def create_configuration_entry(session, headers, config_key, config_value, notes=None):
    payload = {"configKey": config_key, "configValue": config_value}
    if notes is not None:
        payload["notes"] = notes
    resp = session.post(CONFIG_URL, json=payload, headers=headers, timeout=TIMEOUT)
    if resp.status_code == 200:
        json_data = resp.json()
        assert json_data.get("success") is True
        data = json_data["data"]
        assert data["configKey"] == config_key
        assert data["configValue"] == config_value
        if notes is not None:
            assert data.get("notes") == notes
        return True
    elif resp.status_code == 400:
        # Duplicate key or validation error
        return False
    else:
        resp.raise_for_status()


def delete_configuration_entry(session, headers, config_key):
    del_url = f"{CONFIG_URL}/{config_key}"
    resp = session.delete(del_url, headers=headers, timeout=TIMEOUT)
    if resp.status_code not in (204, 404):
        resp.raise_for_status()


def test_get_configuration_entry_by_key():
    session = requests.Session()
    token = authenticate()
    headers = {"Authorization": f"Bearer {token}"}

    # Use a unique configKey to ensure predictable test
    import uuid

    config_key = f"TEST_CONFIG_{uuid.uuid4().hex[:8]}"
    config_value = "test-value"
    notes = "Test notes for retrieval"

    created = create_configuration_entry(session, headers, config_key, config_value, notes)
    try:
        # GET existing configuration - expect 200
        get_url = f"{CONFIG_URL}/{config_key}"
        resp = session.get(get_url, headers=headers, timeout=TIMEOUT)
        assert resp.status_code == 200
        json_data = resp.json()
        assert json_data.get("success") is True
        data = json_data["data"]
        assert data["configKey"] == config_key
        assert data["configValue"] == config_value
        assert data.get("notes") == notes

        # GET non-existent configuration - expect 404
        fake_key = f"NONEXISTENT_{uuid.uuid4().hex[:8]}"
        get_url_fake = f"{CONFIG_URL}/{fake_key}"
        resp_fake = session.get(get_url_fake, headers=headers, timeout=TIMEOUT)
        assert resp_fake.status_code == 404
    finally:
        if created:
            delete_configuration_entry(session, headers, config_key)


test_get_configuration_entry_by_key()