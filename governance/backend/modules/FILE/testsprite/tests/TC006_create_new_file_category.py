import requests

BASE_URL = "http://localhost:7272/actuator/health"
LOGIN_URL = "http://localhost:7272/api/v1/security/auth/login"
CREATE_CATEGORY_PATH = "/api/v1/files/categories"
DELETE_CATEGORY_PATH = "/api/v1/files/categories/{id}"

def get_access_token(username: str, password: str, timeout: int = 30) -> str:
    response = requests.post(
        LOGIN_URL,
        json={"username": username, "password": password},
        timeout=timeout,
    )
    response.raise_for_status()
    json_resp = response.json()
    assert json_resp.get("success") is True, "Login failed: success flag is false"
    access_token = json_resp["data"].get("accessToken")
    assert access_token, "No accessToken found in login response"
    return access_token

def test_create_new_file_category():
    token = get_access_token("admin", "admin")
    headers = {
        "Authorization": f"Bearer {token}",
        "Content-Type": "application/json",
    }

    # Prepare payload for creating the new file category
    # Required fields: categoryCode (max 50), nameAr(max 200), nameEn(max 100)
    payload = {
        "categoryCode": "TEST_CAT_CODE_TC006",
        "nameAr": "اختبار الفئة",
        "nameEn": "Test Category",
        # Optional fields also can be included but omitted here for minimal required test
    }

    category_id = None
    try:
        # Create the file category
        create_resp = requests.post(
            BASE_URL.replace("/actuator/health", "") + CREATE_CATEGORY_PATH,
            json=payload,
            headers=headers,
            timeout=30,
        )
        create_resp.raise_for_status()
        create_json = create_resp.json()
        assert create_json.get("success") is True, "API did not return success for create"
        data = create_json.get("data")
        assert data is not None, "Response data is missing"
        category_id = data.get("id")
        assert category_id is not None, "Created category ID missing in response"

        # Validate returned fields match the sent data
        assert data.get("categoryCode") == payload["categoryCode"]
        assert data.get("nameAr") == payload["nameAr"]
        assert data.get("nameEn") == payload["nameEn"]

    finally:
        # Cleanup: delete the created category if it was created
        if category_id is not None:
            delete_resp = requests.delete(
                BASE_URL.replace("/actuator/health", "") + DELETE_CATEGORY_PATH.format(id=category_id),
                headers=headers,
                timeout=30,
            )
            # Accept both 204 and 200 as successful deletion per spec (204 preferred)
            assert delete_resp.status_code in (204, 200), f"Failed to delete category id {category_id}"

test_create_new_file_category()