import test from "node:test";
import assert from "node:assert/strict";
import {execute} from "../kimi-webbridge-agent.mjs";

test("公开协议按导航、快照、可见岗位事实补充执行并保持同一任务会话", async () => {
  const calls = [];
  const results = [{success:true}, {url:"https://www.zhipin.com/web/geek/jobs", tree:[]}, {value:JSON.stringify({cards:[],detail:{company:null,jd:null}})}];
  const result = await execute({session:"roleos-fixture", operation:"navigate", arguments:{url:"https://www.zhipin.com/"}}, async (url, options) => {
    calls.push(JSON.parse(options.body));
    assert.equal(url, "http://127.0.0.1:10086/command");
    return {ok:true, json:async () => ({ok:true,data:results.shift()})};
  });
  assert.deepEqual(calls.map(c=>c.action), ["navigate","snapshot","evaluate"]);
  assert.ok(calls.every(c=>c.session === "roleos-fixture"));
  assert.deepEqual(JSON.parse(result.accessibilityText), {tree:[],cards:[],detail:{company:null,jd:null}});
});

test("验证码仅返回人工暂停信号，不继续读取或操作", async () => {
  let calls = 0;
  const result = await execute({session:"roleos-fixture",operation:"snapshot"}, async () => {
    calls++;
    return {ok:true,json:async () => ({ok:true,data:{url:"https://www.zhipin.com/",tree:[{role:"heading",name:"安全验证"}]}})};
  });
  assert.equal(calls,1);
  assert.equal(result.humanVerificationRequired,true);
});

test("拒绝任意网站与通用浏览操作，失败不自动重试", async () => {
  const fail = async () => {throw new Error("不得调用");};
  await assert.rejects(execute({session:"roleos-fixture",operation:"navigate",arguments:{url:"https://example.com/"}},fail),/UNTRUSTED_URL/);
  await assert.rejects(execute({session:"roleos-fixture",operation:"click"},fail),/UNSUPPORTED_OPERATION/);
  let calls = 0;
  await assert.rejects(execute({session:"roleos-fixture",operation:"snapshot"},async () => {
    calls++;
    return {ok:false,json:async () => ({ok:false,error:{message:"private"}})};
  }),/WEBBRIDGE_COMMAND_FAILED/);
  assert.equal(calls,1);
});
