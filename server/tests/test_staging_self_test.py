import os

import pytest
from fastapi import HTTPException
from fastapi.testclient import TestClient

import app as relay_app


def test_staging_self_test_disabled_by_default(monkeypatch):
    monkeypatch.delenv("JARVIS_STAGING_SELF_TEST_ENABLED", raising=False)
    response = TestClient(relay_app.app).post(
        "/__staging/self-test",
        headers={"Authorization": "Bearer " + "r" * 40},
    )
    assert response.status_code == 404


def test_staging_self_test_requires_relay_auth_and_returns_no_secrets(monkeypatch):
    monkeypatch.setenv("JARVIS_STAGING_SELF_TEST_ENABLED", "true")
    monkeypatch.setenv("JARVIS_RELAY_TOKEN", "r" * 40)
    monkeypatch.setenv("JARVIS_CLOUDFLARE_RELAY_URL", "https://adapter.example.workers.dev")
    monkeypatch.setenv("JARVIS_CLOUDFLARE_RELAY_TOKEN", "c" * 40)
    monkeypatch.delenv("OPENAI_API_KEY", raising=False)
    monkeypatch.setattr(relay_app, "client", None)
    monkeypatch.setattr(
        relay_app,
        "call_cloudflare_relay",
        lambda req, endpoint, secret: relay_app.AssistantResponse(text="test reply", actions=[]),
    )

    client = TestClient(relay_app.app)
    unauthorized = client.post("/__staging/self-test")
    assert unauthorized.status_code == 401

    response = client.post(
        "/__staging/self-test",
        headers={"Authorization": "Bearer " + "r" * 40},
    )
    assert response.status_code == 200
    server_to_server = client.post(
        "/__staging/self-test",
        headers={"Authorization": "Bearer " + "c" * 40},
    )
    assert server_to_server.status_code == 200
    invalid = client.post(
        "/__staging/self-test",
        headers={"Authorization": "Bearer " + "x" * 40},
    )
    assert invalid.status_code == 401
    payload = response.json()
    assert payload == {
        "status": "ok",
        "stage": "staging",
        "checks": {
            "relay_auth": True,
            "cloudflare_adapter_auth_and_forwarding": True,
            "response_schema": True,
            "non_empty_reply": True,
        },
    }
    assert "r" * 40 not in response.text
    assert "c" * 40 not in response.text


def test_staging_self_test_fails_closed_when_cloudflare_config_missing(monkeypatch):
    monkeypatch.setenv("JARVIS_STAGING_SELF_TEST_ENABLED", "true")
    monkeypatch.setenv("JARVIS_RELAY_TOKEN", "r" * 40)
    monkeypatch.delenv("JARVIS_CLOUDFLARE_RELAY_URL", raising=False)
    monkeypatch.delenv("JARVIS_CLOUDFLARE_RELAY_TOKEN", raising=False)
    response = TestClient(relay_app.app).post(
        "/__staging/self-test",
        headers={"Authorization": "Bearer " + "r" * 40},
    )
    assert response.status_code == 503


def test_staging_web_client_is_opt_in_and_does_not_embed_credentials(monkeypatch):
    client = TestClient(relay_app.app)
    monkeypatch.setenv("JARVIS_STAGING_SELF_TEST_ENABLED", "true")
    monkeypatch.delenv("JARVIS_WEB_TEST_CLIENT_ENABLED", raising=False)
    assert client.get("/__staging/test-client").status_code == 404

    monkeypatch.setenv("JARVIS_WEB_TEST_CLIENT_ENABLED", "true")
    response = client.get("/__staging/test-client")
    assert response.status_code == 200
    assert response.headers["cache-control"].startswith("no-store")
    assert response.headers["content-security-policy"]
    assert "JARVIS_WEB_TEST_TOKEN" not in response.text
    assert "localStorage" not in response.text
    assert "sessionStorage" not in response.text


def test_expired_web_test_token_is_rejected(monkeypatch):
    monkeypatch.setenv("JARVIS_STAGING_SELF_TEST_ENABLED", "true")
    monkeypatch.setenv("JARVIS_RELAY_TOKEN", "r" * 40)
    monkeypatch.setenv("JARVIS_WEB_TEST_TOKEN", "w" * 48)
    monkeypatch.setenv("JARVIS_WEB_TEST_TOKEN_EXPIRES_AT", "1")
    monkeypatch.setenv("JARVIS_CLOUDFLARE_RELAY_URL", "https://adapter.example.workers.dev")
    monkeypatch.setenv("JARVIS_CLOUDFLARE_RELAY_TOKEN", "c" * 40)
    response = TestClient(relay_app.app).post(
        "/__staging/self-test",
        headers={"Authorization": "Bearer " + "w" * 48},
    )
    assert response.status_code == 401
