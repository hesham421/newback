import requests
import uuid

BASE_URL = "http://localhost:7272"
TIMEOUT = 30


def authenticate():
    url = f"{BASE_URL}/api/v1/security/auth/login"
    payload = {"username": "admin", "password": "admin"}
    response = requests.post(url, json=payload, timeout=TIMEOUT)
    response.raise_for_status()
    json_resp = response.json()
    assert json_resp.get("success") is True, "Authentication failed"
    access_token = json_resp["data"]["accessToken"]
    return access_token


def create_file_category(headers):
    url = f"{BASE_URL}/api/v1/files/categories"
    category_code = "CAT_" + uuid.uuid4().hex[:8].upper()
    payload = {
        "categoryCode": category_code,
        "nameAr": "فئة اختبار",
        "nameEn": "Test Category",
        "maxSizeBytes": 1000000,
        "allowedContentTypes": "application/pdf,image/png",
        "isActiveFl": True
    }
    response = requests.post(url, json=payload, headers=headers, timeout=TIMEOUT)
    response.raise_for_status()
    json_resp = response.json()
    assert json_resp.get("success") is True, "File category creation failed"
    category = json_resp["data"]
    return category


def delete_file_category(category_id, headers):
    url = f"{BASE_URL}/api/v1/files/categories/{category_id}"
    response = requests.delete(url, headers=headers, timeout=TIMEOUT)
    if response.status_code not in (204, 404):
        response.raise_for_status()


def test_search_file_categories_with_pagination():
    token = authenticate()
    headers = {
        "Authorization": f"Bearer {token}",
        "Content-Type": "application/json"
    }

    # Create a category to ensure the search returns at least one record
    category = create_file_category(headers)
    category_id = category["id"]

    try:
        search_url = f"{BASE_URL}/api/v1/files/categories/search"
        search_payload = {
            "filters": [
                {"field": "categoryCode", "operator": "EQUALS", "value": category["categoryCode"]},
                {"field": "isActive", "operator": "EQUALS", "value": True}
            ],
            "page": 0,
            "size": 10
        }
        response = requests.post(search_url, json=search_payload, headers=headers, timeout=TIMEOUT)
        response.raise_for_status()
        json_resp = response.json()
        assert json_resp.get("success") is True, "Search response 'success' flag false"
        data = json_resp["data"]
        # Validate pagination keys
        assert "content" in data, "Pagination 'content' missing in response data"
        assert isinstance(data["content"], list), "'content' should be a list"
        assert data.get("pageable") is not None, "'pageable' missing in pagination data"
        assert data.get("totalElements") is not None, "'totalElements' missing in pagination data"
        assert data.get("totalPages") is not None, "'totalPages' missing in pagination data"
        # Check that the created category exists in the results
        found = any(cat["id"] == category_id for cat in data["content"])
        assert found, "Created category not found in search results"
    finally:
        # Cleanup the created category
        delete_file_category(category_id, headers)


test_search_file_categories_with_pagination()