import requests
import uuid

BASE_URL = "http://localhost:7272"
TIMEOUT = 30

def test_post_api_v1_sec_auth_signup_with_duplicate_email():
    signup_url = f"{BASE_URL}/api/v1/sec/auth/signup"
    
    # Generate unique email and names for initial signup
    unique_email = f"duplicate_test_{uuid.uuid4().hex}@example.com"
    signup_data = {
        "email": unique_email,
        "fullNameAr": "اختبار مكرر",
        "fullNameEn": "Duplicate Test"
    }
    
    # First signup attempt - should succeed with 200 or 201 and status PENDING
    try:
        response_first = requests.post(signup_url, json=signup_data, timeout=TIMEOUT)
    except requests.RequestException as e:
        assert False, f"Initial signup request failed: {e}"
    
    assert response_first.status_code in (200, 201), f"Expected 200 or 201 on first signup, got {response_first.status_code}"
    json_first = response_first.json()
    assert json_first.get("success") is True, f"First signup success expected True, got {json_first.get('success')}"
    data_first = json_first.get("data")
    assert data_first is not None and isinstance(data_first, dict), "First signup missing 'data' field"
    assert data_first.get("statusCode") == "PENDING", f"First signup statusCode expected 'PENDING', got {data_first.get('statusCode')}"

    # Second signup attempt with the same email - should fail with 409 and SEC-409-SIGNUP-DUP error code
    try:
        response_second = requests.post(signup_url, json=signup_data, timeout=TIMEOUT)
    except requests.RequestException as e:
        assert False, f"Duplicate signup request failed: {e}"
    
    assert response_second.status_code == 409, f"Expected 409 status for duplicate signup, got {response_second.status_code}"
    json_second = response_second.json()
    assert json_second.get("success") is False, f"Duplicate signup success expected False, got {json_second.get('success')}"
    error = json_second.get("error")
    assert error is not None and isinstance(error, dict), "Duplicate signup missing 'error' field"
    assert error.get("code") == "SEC-409-SIGNUP-DUP", f"Duplicate signup error code expected 'SEC-409-SIGNUP-DUP', got {error.get('code')}"

test_post_api_v1_sec_auth_signup_with_duplicate_email()
