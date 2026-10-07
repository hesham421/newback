import requests
import uuid
import io

BASE_URL = "http://localhost:7272"
LOGIN_URL = f"{BASE_URL}/api/v1/security/auth/login"
FILE_UPLOAD_URL = f"{BASE_URL}/api/v1/files"

def test_post_api_v1_files_with_valid_multipart_data():
    # Step 1: Authenticate to get bearer token
    login_payload = {
        "username": "admin",
        "password": "admin"
    }
    login_resp = requests.post(LOGIN_URL, json=login_payload, timeout=30)
    assert login_resp.status_code == 200, f"Login failed: {login_resp.text}"
    login_json = login_resp.json()
    access_token = login_json.get("data", {}).get("accessToken")
    assert access_token, "accessToken not found in login response"

    headers = {
        "Authorization": f"Bearer {access_token}"
    }

    # Prepare unique moduleCode to avoid duplicate conflicts
    unique_module_code = f"TESTMOD_{uuid.uuid4()}"
    # Use dummy ownerId and ownerType (these must be valid in the system for the test to pass)
    # Since ownerId type is not specified, assume integer 1 for test as example
    owner_id = "1"
    owner_type = "MODULE"

    # Prepare a small in-memory file to upload
    file_content = b"Test file content for upload"
    file_name = "testfile.txt"
    file_obj = io.BytesIO(file_content)

    # Construct multipart form-data with file and form fields
    files = {
        "file": (file_name, file_obj, "text/plain")
    }
    data = {
        "ownerId": owner_id,
        "ownerType": owner_type,
        "moduleCode": unique_module_code
    }

    try:
        upload_resp = requests.post(FILE_UPLOAD_URL, headers=headers, files=files, data=data, timeout=30)
        assert upload_resp.status_code in (200, 201), f"File upload failed: {upload_resp.status_code} {upload_resp.text}"
        json_resp = upload_resp.json()
        assert "data" in json_resp, f"Response missing 'data': {upload_resp.text}"

        file_metadata = json_resp["data"]
        # Validate expected file metadata fields presence
        assert isinstance(file_metadata.get("id"), int) or isinstance(file_metadata.get("id"), str), "File metadata missing 'id'"
        assert file_metadata.get("ownerId") == int(owner_id) or str(file_metadata.get("ownerId")) == owner_id, "ownerId mismatch"
        assert file_metadata.get("ownerType") == owner_type, "ownerType mismatch"
        assert file_metadata.get("moduleCode") == unique_module_code, "moduleCode mismatch"
        assert file_metadata.get("fileName") == file_name or file_metadata.get("fileName") == file_name.lower(), "fileName mismatch"

    finally:
        # Cleanup: if file uploaded, delete it using DELETE endpoint
        file_id = None
        try:
            # Attempt to retrieve uploaded file id if present in metadata
            file_id = file_metadata.get("id") if 'file_metadata' in locals() else None
        except Exception:
            file_id = None

        if file_id:
            delete_url = f"{FILE_UPLOAD_URL}/{file_id}"
            del_resp = requests.delete(delete_url, headers=headers, timeout=30)
            # According to PRD, delete returns 200 with archived/deleted file metadata
            if del_resp.status_code != 200:
                # If deletion fails, raise for visibility
                raise AssertionError(f"Cleanup failed to delete file id {file_id}: {del_resp.status_code} {del_resp.text}")

test_post_api_v1_files_with_valid_multipart_data()