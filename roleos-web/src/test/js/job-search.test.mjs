import test from "node:test";
import assert from "node:assert/strict";
import {searchPresentation, searchRequest} from "../../main/resources/static/jobs/assets/search.mjs";

test("人工暂停和可重试失败提供显式恢复，终态不提供恢复", () => {
  const paused = searchPresentation({state: "PAUSED_FOR_HUMAN", step: "DETAIL_FETCH", waitReason: "HUMAN_VERIFICATION_REQUIRED"});
  assert.equal(paused.resumable, true);
  assert.match(paused.resumeLabel, /已在浏览器完成人工处理/);
  assert.match(paused.text, /HUMAN_VERIFICATION_REQUIRED/);
  assert.equal(searchPresentation({state: "RETRYABLE_FAILED"}).resumable, true);
  for (const state of ["CREATED", "RUNNING", "COMPLETED", "FAILED"]) {
    assert.equal(searchPresentation({state}).resumable, false);
  }
});

test("命令携带 CSRF 和幂等键，读取不发送写命令", async () => {
  const calls = [];
  const fetcher = async (...args) => {
    calls.push(args);
    return {ok: true, json: async () => ({data: {id: "search"}})};
  };
  await searchRequest(fetcher, "/api/job-searches", {
    body: {keywords: ["Java"]}, commandId: "command-id", csrfHeader: "X-CSRF-TOKEN", csrfToken: "fixture"
  });
  assert.equal(calls[0][1].method, "POST");
  assert.equal(calls[0][1].headers["X-CSRF-TOKEN"], "fixture");
  assert.equal(calls[0][1].headers["Idempotency-Key"], "command-id");
  await searchRequest(fetcher, "/api/job-searches/search");
  assert.equal(calls[1][1].method, "GET");
  assert.equal(calls[1][1].headers["Idempotency-Key"], undefined);
});

test("连接失败不会自动重试命令，服务端拒绝会展示错误", async () => {
  let calls = 0;
  await assert.rejects(searchRequest(async () => {
    calls++;
    throw new Error("网络不可达");
  }, "/api/job-searches", {commandId: "command-id"}), /网络不可达/);
  assert.equal(calls, 1);
  await assert.rejects(searchRequest(async () => ({
    ok: false, json: async () => ({message: "需要完成身份认证"})
  }), "/api/job-searches/search"), /需要完成身份认证/);
});
