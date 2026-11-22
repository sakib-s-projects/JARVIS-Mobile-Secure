import os\nimport pytest

os.environ.setdefault("OPENAI_API_KEY", "sk-test-placeholder-key")
from fastapi import HTTPException
from app import check_auth


def test_missing_token_fails_closed(monkeypatch):
    monkeypatch.delenv("ORBIT_RELAY_TOKEN", raising=False)
    with pytest.raises(HTTPException) as exc:
        check_auth("Bearer anything")
    assert exc.value.status_code == 503


def test_blank_token_fails_closed(monkeypatch):
    monkeypatch.setenv("ORBIT_RELAY_TOKEN", " " * 40)
    with pytest.raises(HTTPException) as exc:
        check_auth("Bearer anything")
    assert exc.value.status_code == 503


def test_short_token_fails_closed(monkeypatch):
    monkeypatch.setenv("ORBIT_RELAY_TOKEN", "short")
    with pytest.raises(HTTPException) as exc:
        check_auth("Bearer short")
    assert exc.value.status_code == 503


def test_missing_authorization_rejected(monkeypatch):
    monkeypatch.setenv("ORBIT_RELAY_TOKEN", "a" * 32)
    with pytest.raises(HTTPException) as exc:
        check_auth(None)
    assert exc.value.status_code == 401


def test_wrong_token_rejected(monkeypatch):
    monkeypatch.setenv("ORBIT_RELAY_TOKEN", "a" * 32)
    with pytest.raises(HTTPException) as exc:
        check_auth("Bearer " + "b" * 32)
    assert exc.value.status_code == 401


def test_correct_token_accepted(monkeypatch):
    token = "a" * 32
    monkeypatch.setenv("ORBIT_RELAY_TOKEN", token)
    check_auth("Bearer " + token)


from pydantic import ValidationError
from app import AssistantRequest


def test_request_metadata_limits():
    with pytest.raises(ValidationError):
        AssistantRequest(prompt='ok', timezone='x' * 129)
    with pytest.raises(ValidationError):
        AssistantRequest(prompt='ok', locale='x' * 65)
    with pytest.raises(ValidationError):
        AssistantRequest(prompt='ok', capabilities=['cap'] * 33)


def test_screenshot_payload_limit():
    with pytest.raises(ValidationError):
        AssistantRequest(prompt='ok', screenshotBase64='x' * (4 * 1024 * 1024 + 1))
