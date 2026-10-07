import requests
import uuid
import time

BASE_URL = "http://localhost:7272"
TIMEOUT = 30

def test_post_api_v1_sec_auth_password_reset_complete_with_valid_token():
    # Step 1: Create a new user to reset password for
    unique_id = str(uuid.uuid4()).replace("-", "")[:12]
    new_email = f"testuser_{unique_id}@example.com"
    new_fullNameAr = f"اختبار{unique_id}"
    new_fullNameEn = f"TestUser{unique_id}"
    initial_password = "InitialPass123!"
    updated_password = "UpdatedPass123!"

    # Signup user (public endpoint, no auth required)
    signup_url = f"{BASE_URL}/api/v1/sec/auth/signup"
    signup_payload = {
        "email": new_email,
        "fullNameAr": new_fullNameAr,
        "fullNameEn": new_fullNameEn
    }
    resp = requests.post(signup_url, json=signup_payload, timeout=TIMEOUT)
    assert resp.status_code in (200, 201), f"Signup status code unexpected: {resp.status_code}"
    signup_data = resp.json()
    assert signup_data.get("success") is True
    assert signup_data.get("data", {}).get("statusCode") == "PENDING"

    try:
        # Step 2: Authenticate as admin to approve the signup request to create user
        login_url = f"{BASE_URL}/api/v1/sec/auth/login"
        admin_credentials = {"username": "admin", "password": "admin"}
        login_resp = requests.post(login_url, json=admin_credentials, timeout=TIMEOUT)
        assert login_resp.status_code == 200
        login_json = login_resp.json()
        assert login_json.get("success") is True
        access_token = login_json["data"]["accessToken"]
        headers = {"Authorization": f"Bearer {access_token}"}

        # Step 3: Search signup requests to find the pending request for our email
        search_signup_requests_url = f"{BASE_URL}/api/v1/sec/signup-requests/search"
        search_payload = {
            "page": 0,
            "size": 10,
            "filters": [
                {"field": "email", "operator": "EQ", "value": new_email}
            ]
        }
        # searching signup-requests is not explicitly in the PRD endpoints, so instead
        # we try to directly PATCH on the signup request by the id. We have no direct id;
        # But the PRD allows PATCH /api/v1/sec/signup-requests/{id} with approval decision
        # Without an endpoint to search signup requests, we must try another approach.

        # Instead, since the API only exposes PATCH /api/v1/sec/signup-requests/{id},
        # but no search endpoint for signup requests, we will bypass approval step by creating user directly:
        # According to the PRD, signup creates a PENDING SignupRequest only; so for the purpose of this test,
        # we assume user exists and password reset request can be done with created user email.

        # Step 4: Create user directly via /api/v1/sec/users (to ensure user exists for password reset)
        create_user_url = f"{BASE_URL}/api/v1/sec/users"
        create_user_payload = {
            "username": f"user_{unique_id}",
            "email": new_email,
            "fullNameAr": new_fullNameAr,
            "fullNameEn": new_fullNameEn,
            "password": initial_password
        }
        create_resp = requests.post(create_user_url, json=create_user_payload, headers=headers, timeout=TIMEOUT)
        assert create_resp.status_code == 201, f"User creation failed: {create_resp.text}"
        user_id = create_resp.json()["data"]["userPk"]

        # Step 5: Request a password reset token for the user email (public endpoint)
        password_reset_request_url = f"{BASE_URL}/api/v1/sec/auth/password-reset/request"
        password_reset_request_payload = {"email": new_email}
        reset_req_resp = requests.post(password_reset_request_url, json=password_reset_request_payload, timeout=TIMEOUT)
        assert reset_req_resp.status_code == 200
        reset_req_json = reset_req_resp.json()
        assert reset_req_json.get("success") is True

        # Step 6: Retrieve the password reset token via an assumed endpoint or data source
        # PRD does not provide a direct API to get the token for testing - typically would be sent by email.
        # Since we can't get the token externally, we assume a test-only endpoint exists to fetch active tokens,
        # or we simulate test environment by getting the token from a hypothetical search API.

        # For testing purposes: simulate retrieving token by calling a /api/v1/sec/auth/password-reset/token API (not in PRD)
        # Instead, we will assume that the token is returned or accessible. Since no endpoint exists,
        # to respect instructions, we create a new user and simulate that step via password-reset/request and then
        # also simulate that the token is 'valid-token-for-testing' or we create a custom test token.

        # Without an actual token retrieval API, redefine the test as:
        # Create the token externally or mock it. But we must use the API only.

        # So, as a practical workaround: use a "forgot password" flow expecting token received via email,
        # but since we can't capture email, create a user and a password reset token manually via user creation API.

        # The test instructions do not mention creating tokens manually.
        # So, we'll create a password reset token by triggering password reset request,
        # and assume for test environment the token string is accessible via a test-only endpoint:

        # Let's code a helper function to try fetching tokens (not in PRD),
        # skipping proper retrieval, instead, we fail gracefully if token not available.

        # Test workaround: We'll create a new password reset token on-demand (e.g. 'dummy-valid-token')
        # In an actual environment, this would be retrieved from DB or admin API.

        # Use a placeholder token value as we cannot fetch the real token.
        valid_token = None

        # Attempt to retrieve the token by polling a test endpoint (fake, for demonstration)
        # If such endpoint does not exist, skip this part and raise error.
        token_lookup_url = f"{BASE_URL}/api/v1/sec/test/password-reset-tokens"
        try:
            token_resp = requests.get(token_lookup_url, params={"email": new_email}, timeout=5)
            if token_resp.status_code == 200:
                tokens_json = token_resp.json()
                if tokens_json.get("success") and tokens_json.get("data"):
                    tokens = tokens_json["data"]
                    # Use most recent unused token
                    for t in tokens:
                        if not t.get("used"):
                            valid_token = t.get("token")
                            break
        except Exception:
            # Token retrieval is not supported in prod, continue with None
            pass

        assert valid_token is not None, "Unable to retrieve valid password reset token for test user"

        # Step 7: Complete the password reset with the valid token and new password (public endpoint)
        password_reset_complete_url = f"{BASE_URL}/api/v1/sec/auth/password-reset/complete"
        complete_payload = {
            "token": valid_token,
            "newPassword": updated_password
        }
        complete_resp = requests.post(password_reset_complete_url, json=complete_payload, timeout=TIMEOUT)
        assert complete_resp.status_code == 200, f"Password reset complete failed: {complete_resp.text}"
        complete_json = complete_resp.json()
        assert complete_json.get("success") is True

        # Step 8: Verify that the password is updated by logging in with new password
        login_new_password_payload = {"username": create_user_payload["username"], "password": updated_password}
        login_resp_new = requests.post(login_url, json=login_new_password_payload, timeout=TIMEOUT)
        assert login_resp_new.status_code == 200
        login_new_json = login_resp_new.json()
        assert login_new_json.get("success") is True
        assert "accessToken" in login_new_json["data"]

        # Step 9: Verify the token is marked as used by attempting to reuse it (should fail)
        reuse_resp = requests.post(password_reset_complete_url, json=complete_payload, timeout=TIMEOUT)
        assert reuse_resp.status_code == 409
        reuse_json = reuse_resp.json()
        assert reuse_json.get("success") is False
        assert reuse_json.get("error", {}).get("code") == "SEC-409-RESET-TOKEN-INVALID"

    finally:
        # Cleanup: delete created user to avoid residue
        try:
            delete_user_url = f"{BASE_URL}/api/v1/sec/users/{user_id}"
            delete_resp = requests.delete(delete_user_url, headers=headers, timeout=TIMEOUT)
            assert delete_resp.status_code == 200
        except Exception:
            pass

test_post_api_v1_sec_auth_password_reset_complete_with_valid_token()