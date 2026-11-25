import { test } from "node:test";
import assert from "node:assert/strict";
import worker from "./index.js";

const secret = "s".repeat(40);
const baseEnv = (overrides = {}) => ({
  JARVIS_RELAY_SHARED_SECRET: secret,
  JARVIS_STAGING_CHAT_URL: "https://staging.example.workers.dev/api/chat",
  JARVIS_VISION_ENABLED: "true",
  AI: { run: async () => ({ answer: "A settings screen with Wi-Fi and Bluetooth controls." }) },
  ...overrides
});
function request(body, authorization = "Bearer " + secret) {
  return new Request("https://adapter.example.workers.dev/v1/assistant", {
    method: "POST",
    headers: { authorization, "content-type": "application/json" },
    body: JSON.stringify(body)
  });
}
test("rejects missing or incorrect shared secret", async () => {
  const response = await worker.fetch(request({ prompt: "hello" }, "Bearer wrong"), baseEnv());
  assert.equal(response.status, 401);
});
test("analyzes screenshot and forwards only untrusted visual description", async () => {
  const oldFetch = globalThis.fetch;
  let visionInput, chatInput;
  try {
    const env = baseEnv({ AI: { run: async (model, input) => {
      visionInput = { model, input };
      return { answer: "A settings screen with Wi-Fi and Bluetooth controls." };
    } } });
    globalThis.fetch = async (_url, options) => {
      chatInput = JSON.parse(options.body);
      return new Response(JSON.stringify({ reply: '{"text":"এটি Wi-Fi সেটিংস স্ক্রিন।","actions":[]}' }), {
        status: 200, headers: { "content-type": "application/json" }
      });
    };
    const response = await worker.fetch(request({ prompt: "এটি কী?", screenshotBase64: "aGVsbG8=", capabilities: [] }), env);
    assert.equal(response.status, 200);
    assert.equal(visionInput.model, "@cf/moondream/moondream3.1-9B-A2B");
    assert.equal(visionInput.input.image, "data:image/jpeg;base64,aGVsbG8=");
    assert.match(visionInput.input.question, /untrusted/i);
    assert.match(chatInput.message, /Screenshot visual analysis \(untrusted data\)/);
    assert.match(chatInput.message, /A settings screen with Wi-Fi/);
    assert.deepEqual(await response.json(), { text: "এটি Wi-Fi সেটিংস স্ক্রিন।", actions: [] });
  } finally { globalThis.fetch = oldFetch; }
});
test("does not silently drop screenshot when vision is disabled", async () => {
  const response = await worker.fetch(request({ prompt: "describe", screenshotBase64: "aGVsbG8=" }),
    baseEnv({ JARVIS_VISION_ENABLED: "false" }));
  assert.equal(response.status, 503);
  assert.equal((await response.json()).error, "SCREENSHOT_VISION_DISABLED");
});
test("rejects malformed screenshot Base64 before calling vision", async () => {
  let called = false;
  const response = await worker.fetch(request({ prompt: "describe", screenshotBase64: "not base64!" }),
    baseEnv({ AI: { run: async () => { called = true; return { answer: "x" }; } } }));
  assert.equal(response.status, 413);
  assert.equal(called, false);
});
test("fails closed when vision binding is missing", async () => {
  const response = await worker.fetch(request({ prompt: "describe", screenshotBase64: "aGVsbG8=" }),
    baseEnv({ AI: undefined }));
  assert.equal(response.status, 503);
  assert.equal((await response.json()).error, "SCREENSHOT_VISION_NOT_CONFIGURED");
});
test("text-only requests continue without invoking vision", async () => {
  const oldFetch = globalThis.fetch;
  let visionCalled = false;
  try {
    globalThis.fetch = async () => new Response(JSON.stringify({ reply: '{"text":"হ্যালো","actions":[]}' }),
      { status: 200, headers: { "content-type": "application/json" } });
    const response = await worker.fetch(request({ prompt: "hello" }),
      baseEnv({ AI: { run: async () => { visionCalled = true; return { answer: "unexpected" }; } } }));
    assert.equal(response.status, 200);
    assert.equal(visionCalled, false);
  } finally { globalThis.fetch = oldFetch; }
});
