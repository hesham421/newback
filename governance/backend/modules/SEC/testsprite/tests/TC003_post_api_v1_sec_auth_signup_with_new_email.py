import requests
import uuid

BASE_URL = "http://localhost:7272"
SIGNUP_PATH = "/api/v1/sec/auth/signup"
TIMEOUT = 30

def test_post_api_v1_sec_auth_signup_with_new_email():
    unique_email = f"testuser_{uuid.uuid4().hex}@example.com"
    payload = {
        "email": unique_email,
        "fullNameAr": "اختبار المستخدم",
        "fullNameEn": "Test User"
    }
    headers = {
        "Content-Type": "application/json"
    }
    url = BASE_URL + SIGNUP_PATH
    try:
        response = requests.post(url, json=payload, headers=headers, timeout=TIMEOUT)
    except requests.RequestException as e:
        assert False, f"Request failed: {e}"

    # The API may return 200 or 201 for successful signup request creation
    assert response.status_code in (200, 201), f"Unexpected status code: {response.status_code}, body: {response.text}"

    try:
        body = response.json()
    except ValueError:
        assert False, f"Response body is not valid JSON: {response.text}"

    # Assert overall API success flag
    assert "success" in body, "Missing 'success' in response"
    assert body["success"] is True, f"API call failed with body: {body}"

    # Assert data and statusCode present
    data = body.get("data")
    assert data is not None, f"'data' is missing in response: {body}"
    assert "statusCode" in data, f"'statusCode' not in data: {data}"
    assert data["statusCode"] == "PENDING", f"Expected statusCode 'PENDING', got: {data['statusCode']}"

test_post_api_v1_sec_auth_signup_with_new_email()