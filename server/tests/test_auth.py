import os
os.environ.setdefault("OPENAI_API_KEY", "sk-test-placeholder-key")

import pytest
from fastapi import HTTPException
from pydantic import ValidationError

from app import check_auth, AssistantRequest, RelayRateLimiter


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


def test_request_metadata_limits():
    with pytest.raises(ValidationError):
        AssistantRequest(prompt="ok", timezone="x" * 129)
    with pytest.raises(ValidationError):
        AssistantRequest(prompt="ok", locale="x" * 65)
    with pytest.raises(ValidationError):
        AssistantRequest(prompt="ok", capabilities=["cap"] * 33)


def test_screenshot_payload_limit():
    with pytest.raises(ValidationError):
        AssistantRequest(prompt="ok", screenshotBase64="x" * (4 * 1024 * 1024 + 1))


def test_rate_limit_blocks_over_limit_and_returns_retry_after():
    now = [100.0]
    limiter = RelayRateLimiter(limit=2, window_seconds=60, clock=lambda: now[0])
    limiter.check("token")
    limiter.check("token")
    with pytest.raises(HTTPException) as exc:
        limiter.check("token")
    assert exc.value.status_code == 429
    assert exc.value.headers["Retry-After"] == "60"


def test_rate_limit_resets_after_window():
    now = [100.0]
    limiter = RelayRateLimiter(limit=1, window_seconds=60, clock=lambda: now[0])
    limiter.check("token")
    now[0] = 160.0
    limiter.check("token")


def test_overall_request_body_limit_returns_413():
    from fastapi import FastAPI
    from fastapi.testclient import TestClient
    from starlette.requests import Request
    from app import RequestBodyLimitMiddleware

    test_app = FastAPI()
    test_app.add_middleware(RequestBodyLimitMiddleware, max_bytes=32)

    @test_app.post("/assistant")
    async def echo(request: Request):
        return {"size": len(await request.body())}

    client = TestClient(test_app)
    assert client.post("/assistant", content=b"x" * 32).status_code == 200
    response = client.post("/assistant", content=b"x" * 33)
    assert response.status_code == 413
    assert response.json()["detail"] == "Request body too large"




def test_chunked_request_body_limit_rejects_oversized_stream():
    import asyncio
    from app import RequestBodyLimitMiddleware

    chunks = [
        {"type": "http.request", "body": b"x" * 20, "more_body": True},
        {"type": "http.request", "body": b"x" * 13, "more_body": False},
    ]
    delivered = []
    async def receive():
        return chunks.pop(0)
    async def send(message):
        delivered.append(message)
    async def downstream(scope, receive, send):
        raise AssertionError("oversized body must be rejected before downstream")

    scope = {"type": "http", "method": "POST", "path": "/assistant", "headers": []}
    asyncio.run(RequestBodyLimitMiddleware(downstream, max_bytes=32)(scope, receive, send))
    assert delivered[0]["status"] == 413


def test_content_length_precheck_rejects_without_reading_body():
    import asyncio
    from app import RequestBodyLimitMiddleware

    delivered = []
    async def receive():
        raise AssertionError("body must not be read when Content-Length exceeds limit")
    async def send(message):
        delivered.append(message)
    async def downstream(scope, receive, send):
        raise AssertionError("oversized body must be rejected before downstream")

    scope = {"type": "http", "method": "POST", "path": "/assistant",
             "headers": [(b"content-length", b"33")]}
    asyncio.run(RequestBodyLimitMiddleware(downstream, max_bytes=32)(scope, receive, send))
    assert delivered[0]["status"] == 413
