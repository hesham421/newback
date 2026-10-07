import requests

BASE_URL = "http://localhost:7272"
LOGIN_PATH = "/api/v1/security/auth/login"
SEARCH_CONFIG_PATH = "/api/v1/common/configurations/search"
CONFIG_PATH = "/api/v1/common/configurations"

USERNAME = "admin"
PASSWORD = "admin"
TIMEOUT = 30

def authenticate():
    url = BASE_URL + LOGIN_PATH
    payload = {"username": USERNAME, "password": PASSWORD}
    response = requests.post(url, json=payload, timeout=TIMEOUT)
    response.raise_for_status()
    json_resp = response.json()
    # Remove success assertion as PRD does not specify it
    assert "data" in json_resp and "accessToken" in json_resp["data"], "Login response missing accessToken"
    access_token = json_resp["data"]["accessToken"]
    return access_token

def create_configuration_entry(headers, config_key, config_value, notes=None):
    url = BASE_URL + CONFIG_PATH
    body = {
        "configKey": config_key,
        "configValue": config_value,
    }
    if notes is not None:
        body["notes"] = notes
    response = requests.post(url, json=body, headers=headers, timeout=TIMEOUT)
    response.raise_for_status()
    json_resp = response.json()
    # Remove success assertion
    assert "data" in json_resp, f"Response missing data for config {config_key} creation"
    data = json_resp["data"]
    assert data["configKey"] == config_key
    assert data["configValue"] == config_value
    return data

def delete_configuration_entry(headers, config_key):
    url = BASE_URL + f"{CONFIG_PATH}/{config_key}"
    response = requests.delete(url, headers=headers, timeout=TIMEOUT)
    # Can be 204 if deleted or 404 if not found, ignore if not found
    if response.status_code not in (204, 404):
        response.raise_for_status()

def test_search_configuration_entries_with_pagination():
    access_token = authenticate()
    headers = {"Authorization": f"Bearer {access_token}"}

    # Create a new configuration entry to ensure at least one config exists to find
    config_key = "TEST_TC002_KEY"
    config_value = "test_value"
    notes = "Test note for TC002"
    created = None

    try:
        created = create_configuration_entry(headers, config_key, config_value, notes)
        assert created is not None

        # Prepare the search payload with a filter that should match the created entry
        search_url = BASE_URL + SEARCH_CONFIG_PATH
        search_payload = {
            "filters": {"configKey": config_key},
            "page": 0,
            "size": 10
        }
        response = requests.post(search_url, json=search_payload, headers=headers, timeout=TIMEOUT)
        assert response.status_code == 200
        json_resp = response.json()
        # Remove success assertion
        assert "data" in json_resp and isinstance(json_resp["data"], dict)
        data = json_resp["data"]

        # The response data is a Page<ConfigurationResponse> - check typical page fields
        assert "content" in data and isinstance(data["content"], list)
        assert "totalElements" in data and isinstance(data["totalElements"], int)
        assert "totalPages" in data and isinstance(data["totalPages"], int)
        assert "number" in data and isinstance(data["number"], int)

        # Verify our created configuration entry is in the content list
        found = False
        for item in data["content"]:
            if item.get("configKey") == config_key:
                found = True
                assert item.get("configValue") == config_value
                break
        assert found, f"Configuration with configKey '{config_key}' not found in search results"
    finally:
        if created:
            delete_configuration_entry(headers, config_key)

test_search_configuration_entries_with_pagination()