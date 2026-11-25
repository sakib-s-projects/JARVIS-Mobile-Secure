import { test } from "node:test";
import assert from "node:assert/strict";
import worker from "./index.js";

const secret = "s".repeat(40);
const baseEnv = (overrides = {}) => ({
  JARVIS_RELAY_SHARED_SECRET: secret,
  JARVIS_VISION_ENABLED: "true",
  JARVIS_STAGING_CHAT: {
    fetch: async request => globalThis.fetch(request.url, {
      method: request.method,
      headers: Object.fromEntries(request.headers),
      body: await request.clone().text()
    })
  },
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

test("rejects oversized chunked request bodies before JSON parsing", async () => {
  const stream = new ReadableStream({
    start(controller) {
      controller.enqueue(new Uint8Array(5 * 1024 * 1024 + 1));
      controller.close();
    }
  });
  const req = new Request("https://adapter.example.workers.dev/v1/assistant", {
    method: "POST",
    headers: { authorization: "Bearer " + secret, "content-type": "application/json" },
    body: stream,
    duplex: "half"
  });
  const response = await worker.fetch(req, baseEnv());
  assert.equal(response.status, 413);
  assert.equal((await response.json()).error, "REQUEST_TOO_LARGE");
});


test("bounds oversized optional context while preserving the user request", async () => {
  const oldFetch = globalThis.fetch;
  let chatInput;
  try {
    globalThis.fetch = async (_url, options) => {
      chatInput = JSON.parse(options.body);
      return new Response(JSON.stringify({ reply: '{"text":"ঠিক আছে","actions":[]}' }), {
        status: 200, headers: { "content-type": "application/json" }
      });
    };
    const response = await worker.fetch(request({
      prompt: "এই স্ক্রিনে কী দেখাচ্ছে?",
      systemPrompt: "নীতি ".repeat(900),
      screenText: "দীর্ঘ স্ক্রিন টেক্সট ".repeat(12000),
      notificationContext: "notification ".repeat(1000),
      memoryContext: "memory ".repeat(1000),
      capabilities: []
    }), baseEnv());
    assert.equal(response.status, 200);
    assert.ok(chatInput.message.length <= 12000);
    assert.match(chatInput.message, /এই স্ক্রিনে কী দেখাচ্ছে\?/);
    assert.match(chatInput.message, /নীতি/);
    assert.match(chatInput.message, /context clipped/);
  } finally { globalThis.fetch = oldFetch; }
});

test("rejects mandatory policy and user prompt that cannot fit the provider limit", async () => {
  const oldFetch = globalThis.fetch;
  let upstreamCalled = false;
  try {
    globalThis.fetch = async () => {
      upstreamCalled = true;
      return new Response("{}", { status: 200 });
    };
    const response = await worker.fetch(request({
      prompt: "প্রশ্ন ".repeat(1200),
      systemPrompt: "নীতি ".repeat(1200)
    }), baseEnv());
    assert.equal(response.status, 413);
    assert.equal((await response.json()).error, "PROMPT_TOO_LARGE_FOR_STAGING_PROVIDER");
    assert.equal(upstreamCalled, false);
  } finally { globalThis.fetch = oldFetch; }
});
