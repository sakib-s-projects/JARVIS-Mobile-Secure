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
