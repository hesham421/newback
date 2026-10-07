import requests

base_url = "http://localhost:7272"

def test_upload_file_with_metadata():
    session = requests.Session()
    try:
        # Authenticate and get access token
        auth_url = f"{base_url}/api/v1/security/auth/login"
        auth_payload = {"username": "admin", "password": "admin"}
        auth_resp = session.post(auth_url, json=auth_payload, timeout=30)
        auth_resp.raise_for_status()
        auth_json = auth_resp.json()
        assert auth_json.get("success") is True
        access_token = auth_json["data"]["accessToken"]
        headers = {"Authorization": f"Bearer {access_token}"}

        # Prepare file upload
        upload_url = f"{base_url}/api/v1/files"
        file_content = b"Test file content for upload_file_with_metadata"
        files = {
            "file": ("testfile.txt", file_content, "text/plain")
        }
        data = {
            "ownerId": 1001,
            "ownerType": "PURCHASE_ORDER",
            "moduleCode": "PROC"
        }

        # Upload the file with metadata
        upload_resp = session.post(upload_url, headers=headers, files=files, data=data, timeout=30)
        upload_resp.raise_for_status()
        upload_json = upload_resp.json()
        assert upload_json.get("success") is True
        file_metadata = upload_json["data"]

        # Validate presence of file metadata fields (at minimum id, ownerId, ownerType, moduleCode)
        assert "id" in file_metadata and file_metadata["id"]
        assert file_metadata.get("ownerId") == data["ownerId"]
        assert file_metadata.get("ownerType") == data["ownerType"]
        assert file_metadata.get("moduleCode") == data["moduleCode"]

    finally:
        # Cleanup: delete the uploaded file if created
        try:
            if 'file_metadata' in locals() and "id" in file_metadata:
                delete_url = f"{base_url}/api/v1/files/{file_metadata['id']}"
                delete_resp = session.delete(delete_url, headers=headers, timeout=30)
                # It's okay if delete fails; just attempt cleanup
        except Exception:
            pass

test_upload_file_with_metadata()