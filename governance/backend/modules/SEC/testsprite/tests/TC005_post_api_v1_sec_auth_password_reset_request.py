import requests
import uuid

BASE_URL = "http://localhost:7272"
PASSWORD_RESET_REQUEST_URL = f"{BASE_URL}/api/v1/sec/auth/password-reset/request"
USERS_URL = f"{BASE_URL}/api/v1/sec/users"

# Helper function to create a unique user (requires admin token)
def create_unique_user():
    unique_id = str(uuid.uuid4())
    username = f"testuser_{unique_id}"
    email = f"{unique_id}@example.com"
    fullNameAr = f"اسم {unique_id}"
    fullNameEn = f"Name {unique_id}"
    password = "TestPass123!"
    payload = {
        "username": username,
        "email": email,
        "fullNameAr": fullNameAr,
        "fullNameEn": fullNameEn,
        "password": password
    }
    return payload

def test_post_api_v1_sec_auth_password_reset_request():
    # We do not need auth token since this endpoint is public
    # Step 1: Create a unique user to test email exists scenario
    # This endpoint requires auth token, so obtain it
    login_url = f"{BASE_URL}/api/v1/sec/auth/login"
    login_payload = {"username": "admin", "password": "admin"}
    login_resp = requests.post(login_url, json=login_payload, timeout=30)
    assert login_resp.status_code == 200, f"Login failed with status {login_resp.status_code}"
    login_data = login_resp.json()
    assert login_data.get("success") is True, "Login response success != True"
    access_token = login_data["data"]["accessToken"]
    headers_auth = {"Authorization": f"Bearer {access_token}"}

    # Create user
    user_payload = create_unique_user()
    create_user_resp = requests.post(USERS_URL, json=user_payload, headers=headers_auth, timeout=30)
    assert create_user_resp.status_code == 201, f"User creation failed with status {create_user_resp.status_code}"
    create_user_json = create_user_resp.json()
    assert create_user_json.get("success") is True, "User creation response success != True"
    created_user_email = create_user_json["data"]["email"]

    try:
        # Test case requires posting password-reset/request with an email that exists
        # and also with email that does not exist to ensure generic 200 response.

        # Email that exists: the created user's email
        payload_exists = {"email": created_user_email}
        resp_exists = requests.post(PASSWORD_RESET_REQUEST_URL, json=payload_exists, timeout=30)
        assert resp_exists.status_code == 200, f"Password reset request (existing email) failed with status {resp_exists.status_code}"
        json_exists = resp_exists.json()
        assert json_exists.get("success") is True, "Password reset request (existing email) success != True"
        # Confirmation response should be generic, so no sensitive info
        assert "data" in json_exists and json_exists["data"] is not None, "Password reset request (existing email) missing confirmation data"

        # Email that does NOT exist (random)
        random_email = f"not_exists_{uuid.uuid4()}@example.com"
        payload_not_exists = {"email": random_email}
        resp_not_exists = requests.post(PASSWORD_RESET_REQUEST_URL, json=payload_not_exists, timeout=30)
        assert resp_not_exists.status_code == 200, f"Password reset request (non-existing email) failed with status {resp_not_exists.status_code}"
        json_not_exists = resp_not_exists.json()
        assert json_not_exists.get("success") is True, "Password reset request (non-existing email) success != True"
        assert "data" in json_not_exists and json_not_exists["data"] is not None, "Password reset request (non-existing email) missing confirmation data"

        # We cannot verify PasswordResetToken creation by API directly since no such endpoint exposed.
        # This test confirms the API returns 200 with generic confirmation response regardless of email existence.
    finally:
        # Cleanup: delete the created user to avoid duplicates
        user_id = create_user_json["data"].get("id")
        if user_id:
            delete_url = f"{USERS_URL}/{user_id}"
            delete_resp = requests.delete(delete_url, headers=headers_auth, timeout=30)
            # Optionally assert delete success or ignore silently
            # assert delete_resp.status_code == 200, "Failed to delete user after test"

test_post_api_v1_sec_auth_password_reset_request()