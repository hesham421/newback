import requests

BASE_URL = "http://localhost:7272"
TIMEOUT = 30


def test_post_api_v1_sec_auth_password_reset_complete_with_invalid_token():
    url = f"{BASE_URL}/api/v1/sec/auth/password-reset/complete"
    # Use an obviously invalid token (expired or used) for the test
    invalid_token = "expired-or-used-token-1234567890"
    new_password = "NewPassword123!"

    payload = {
        "token": invalid_token,
        "newPassword": new_password
    }
    headers = {
        "Content-Type": "application/json"
    }
    response = requests.post(url, json=payload, headers=headers, timeout=TIMEOUT)
    assert response.status_code == 409, f"Expected status code 409 but got {response.status_code}"
    resp_json = response.json()
    assert resp_json.get("success") is False, "Expected success to be false in error response"
    error = resp_json.get("error")
    assert error is not None, "Expected error object in response"
    assert error.get("code") == "SEC-409-RESET-TOKEN-INVALID", f"Expected error code SEC-409-RESET-TOKEN-INVALID but got {error.get('code')}"

    # No password change can be verified here since we have no user context or old password;
    # The API contract states the password remains unchanged on invalid token,
    # so this test only asserts the correct error response is returned.


test_post_api_v1_sec_auth_password_reset_complete_with_invalid_token()