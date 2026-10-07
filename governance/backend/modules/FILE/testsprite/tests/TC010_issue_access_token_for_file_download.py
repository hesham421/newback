import requests
import io

BASE_URL = "http://localhost:7272"
LOGIN_URL = f"{BASE_URL}/api/v1/security/auth/login"
FILES_URL = f"{BASE_URL}/api/v1/files"


def test_issue_access_token_for_file_download():
    session = requests.Session()
    timeout = 30

    # Authenticate and get access token
    login_payload = {"username": "admin", "password": "admin"}
    login_resp = session.post(LOGIN_URL, json=login_payload, timeout=timeout)
    assert login_resp.status_code == 200, f"Login failed: {login_resp.text}"
    login_json = login_resp.json()
    assert login_json.get("success") is True, "Login success flag false"
    access_token = login_json["data"].get("accessToken")
    assert access_token, "No accessToken in login response"

    headers = {"Authorization": f"Bearer {access_token}"}

    # To test issuing access token for a file, we need a file ID.
    # Because no file ID provided, upload a file first and clean up after.
    file_id = None
    try:
        # Prepare a small dummy file
        file_content = io.BytesIO(b"Test file content for TC010")
        files = {"file": ("testfile.txt", file_content, "text/plain")}
        data = {
            "ownerId": "1001",
            "ownerType": "PURCHASE_ORDER",
            "moduleCode": "PROC"
        }
        upload_resp = session.post(FILES_URL, headers=headers, files=files, data=data, timeout=timeout)
        assert upload_resp.status_code == 201, f"File upload failed: {upload_resp.text}"
        upload_json = upload_resp.json()
        assert upload_json.get("success") is True, "File upload success flag false"
        file_metadata = upload_json["data"]
        file_id = file_metadata.get("id")
        assert file_id, "No file id in upload response"

        # Issue access token for the uploaded file ID
        access_token_url = f"{FILES_URL}/{file_id}/access-token"
        token_resp = session.post(access_token_url, headers=headers, timeout=timeout)
        assert token_resp.status_code == 200, f"Access token request failed: {token_resp.text}"
        token_json = token_resp.json()
        assert token_json.get("success") is True, "Access token success flag false"
        token_data = token_json["data"]
        token = token_data.get("accessToken")
        assert token, "No accessToken returned in access token response"

    finally:
        if file_id:
            # Clean up: delete the uploaded file
            del_resp = session.delete(f"{FILES_URL}/{file_id}", headers=headers, timeout=timeout)
            # Allowed codes: 200 or 404 if already deleted
            assert del_resp.status_code in (200, 404), f"File delete failed: {del_resp.text}"


test_issue_access_token_for_file_download()