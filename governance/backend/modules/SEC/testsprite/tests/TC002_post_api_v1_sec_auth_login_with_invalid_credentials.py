import requests

BASE_URL = "http://localhost:7272"
LOGIN_PATH = "/api/v1/sec/auth/login"
TIMEOUT = 30

def test_post_api_v1_sec_auth_login_with_invalid_credentials():
    url = BASE_URL + LOGIN_PATH
    headers = {"Content-Type": "application/json"}
    # Wrong-credential cases: Bean Validation passes (both fields non-blank),
    # so these reach the credential check and correctly get 401.
    invalid_credentials_list = [
        {"username": "invaliduser", "password": "admin"},
        {"username": "admin", "password": "wrongpassword"},
        {"username": "invaliduser", "password": "wrongpassword"},
    ]
    for creds in invalid_credentials_list:
        try:
            response = requests.post(url, json=creds, headers=headers, timeout=TIMEOUT)
        except requests.RequestException as e:
            assert False, f"Request failed: {e}"

        # Assert status code is 401
        assert response.status_code == 401, f"Expected 401 status but got {response.status_code} for credentials {creds}"

        # Assert response body is a JSON with success:false and error.code = SEC-401-INVALID-CREDENTIALS
        try:
            body = response.json()
        except ValueError:
            assert False, "Response is not valid JSON"

        assert isinstance(body, dict), "Response JSON is not a dict"
        assert body.get("success") is False, f"Expected success=false but got {body.get('success')} for credentials {creds}"
        error = body.get("error")
        assert isinstance(error, dict), "Response error field is not a dict"
        code = error.get("code")
        assert code == "SEC-401-INVALID-CREDENTIALS", f"Expected error code SEC-401-INVALID-CREDENTIALS but got {code} for credentials {creds}"

    # Blank-field cases: Bean Validation rejects a blank required field
    # before the credential check ever runs, so these correctly get 400
    # VALIDATION_ERROR with a fieldErrors entry naming the blank field —
    # not 401. Confirmed live via curl against the running app.
    blank_field_cases = [
        ({"username": "", "password": "admin"}, "username"),
        ({"username": "admin", "password": ""}, "password"),
    ]
    for creds, blank_field in blank_field_cases:
        try:
            response = requests.post(url, json=creds, headers=headers, timeout=TIMEOUT)
        except requests.RequestException as e:
            assert False, f"Request failed: {e}"

        assert response.status_code == 400, f"Expected 400 status but got {response.status_code} for credentials {creds}"

        try:
            body = response.json()
        except ValueError:
            assert False, "Response is not valid JSON"

        assert isinstance(body, dict), "Response JSON is not a dict"
        assert body.get("success") is False, f"Expected success=false but got {body.get('success')} for credentials {creds}"
        error = body.get("error")
        assert isinstance(error, dict), "Response error field is not a dict"
        assert error.get("code") == "VALIDATION_ERROR", f"Expected error code VALIDATION_ERROR but got {error.get('code')} for credentials {creds}"
        field_errors = error.get("fieldErrors")
        assert isinstance(field_errors, list) and len(field_errors) >= 1, f"Expected fieldErrors list but got {field_errors} for credentials {creds}"
        assert any(fe.get("field") == blank_field for fe in field_errors), f"Expected a fieldErrors entry for '{blank_field}' but got {field_errors}"

# Run the test function
test_post_api_v1_sec_auth_login_with_invalid_credentials()