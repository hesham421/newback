import requests

BASE_URL = "http://localhost:7272/actuator/health"
LOGIN_URL = "http://localhost:7272/api/v1/security/auth/login"
CATEGORY_URL = "http://localhost:7272/api/v1/files/categories"
TIMEOUT = 30

def authenticate():
    login_payload = {"username": "admin", "password": "admin"}
    resp = requests.post(
        LOGIN_URL,
        json=login_payload,
        timeout=TIMEOUT,
    )
    resp.raise_for_status()
    body = resp.json()
    assert body.get("success") is True
    access_token = body["data"]["accessToken"]
    return access_token

def create_file_category(headers):
    create_payload = {
        "categoryCode": "TEST_CAT_001",
        "nameAr": "اختبار",
        "nameEn": "Test Category",
        "maxSizeBytes": 10485760,
        "allowedContentTypes": "application/pdf,image/png",
        "isActiveFl": True
    }
    resp = requests.post(
        CATEGORY_URL,
        json=create_payload,
        headers=headers,
        timeout=TIMEOUT,
    )
    resp.raise_for_status()
    body = resp.json()
    assert body.get("success") is True
    data = body["data"]
    return data["id"]

def delete_file_category(category_id, headers):
    resp = requests.delete(
        f"{CATEGORY_URL}/{category_id}",
        headers=headers,
        timeout=TIMEOUT,
    )
    if resp.status_code not in [204, 404]:
        resp.raise_for_status()

def update_file_category_by_id():
    token = authenticate()
    headers = {
        "Authorization": f"Bearer {token}",
        "Content-Type": "application/json",
        "Accept": "application/json",
    }
    category_id = None
    try:
        # Create a new category to update
        category_id = create_file_category(headers)

        update_payload = {
            "nameAr": "تحديث الاختبار",
            "nameEn": "Updated Test Category",
            "maxSizeBytes": 20971520,
            "allowedContentTypes": "application/pdf,image/jpeg"
        }

        resp = requests.put(
            f"{CATEGORY_URL}/{category_id}",
            json=update_payload,
            headers=headers,
            timeout=TIMEOUT,
        )
        assert resp.status_code == 200
        body = resp.json()
        assert body.get("success") is True
        data = body["data"]
        assert data["id"] == category_id
        assert data["nameAr"] == update_payload["nameAr"]
        assert data["nameEn"] == update_payload["nameEn"]
        if "maxSizeBytes" in data:
            assert data["maxSizeBytes"] == update_payload["maxSizeBytes"]
        if "allowedContentTypes" in data:
            assert data["allowedContentTypes"] == update_payload["allowedContentTypes"]
    finally:
        if category_id is not None:
            delete_file_category(category_id, headers)

update_file_category_by_id()