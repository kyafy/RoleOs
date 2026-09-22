/** RoleOS 本地命令适配器：将进程 JSON 契约转换为 Kimi 公开 HTTP 协议。 */
import {pathToFileURL} from "node:url";

const endpoint = "http://127.0.0.1:10086/command";

function trustedUrl(value) {
  const url = new URL(value);
  if (url.protocol !== "https:" || url.hostname !== "www.zhipin.com" || url.username || url.password || (url.port && url.port !== "443")) {
    throw new Error("UNTRUSTED_URL");
  }
  return url.href;
}

export async function execute(input, fetcher = fetch) {
  if (!/^roleos-[a-z0-9-]{1,80}$/.test(input.session || "")) throw new Error("INVALID_SESSION");
  // 002 只读取岗位；该命令边界不开放 evaluate/cdp/click/fill 等通用远程操作。
  if (!["navigate", "snapshot"].includes(input.operation)) throw new Error("UNSUPPORTED_OPERATION");
  const command = async (action, args = {}) => {
    const response = await fetcher(endpoint, {
      method: "POST", headers: {"Content-Type": "application/json"},
      body: JSON.stringify({action, args, session: input.session}), signal: AbortSignal.timeout(20000)
    });
    const envelope = await response.json();
    if (!response.ok || envelope.ok !== true) throw new Error("WEBBRIDGE_COMMAND_FAILED");
    return envelope.data;
  };
  if (input.operation === "navigate") {
    await command("navigate", {url: trustedUrl(input.arguments?.url), group_title: "RoleOS 岗位搜索"});
  }
  const snapshot = await command("snapshot");
  const url = trustedUrl(snapshot.url);
  if (!Array.isArray(snapshot.tree)) throw new Error("INVALID_SNAPSHOT");
  const serialized = JSON.stringify(snapshot.tree);
  const humanVerificationRequired = /验证码|安全验证|风险提示|captcha/i.test(serialized);
  if (humanVerificationRequired) return {url, accessibilityText: serialized, humanVerificationRequired, elementRefs: []};
  // Accessibility Snapshot 不含 href，也无法保证链接与卡片的父子关系。仅补充当前可见卡片
  // 和详情页中的公开岗位事实；绝不读取 Cookie、账户菜单、浏览器存储或隐藏节点。
  const facts = await command("evaluate", {
    code: 'JSON.stringify((()=>{const visible=e=>e.getClientRects().length>0;const text=s=>document.querySelector(s)?.innerText?.trim()||null;const cards=Array.from(document.querySelectorAll("li.job-card-box")).filter(visible).map(card=>{const link=card.querySelector("a.job-name[href*=\\"/job_detail/\\"]");if(!link||!visible(link))return null;return {title:link.innerText.trim()||null,url:link.href,salary:card.querySelector(".job-salary")?.innerText?.trim()||null,tags:Array.from(card.querySelectorAll(".tag-list li")).map(item=>item.innerText.trim()).filter(Boolean),location:card.querySelector(".company-location")?.innerText?.trim()||null};}).filter(Boolean);const section=Array.from(document.querySelectorAll(".job-detail-section")).find(item=>item.querySelector("h3")?.innerText?.trim()==="职位描述");return {cards,detail:{company:text(".company-info .company-name")||text(".company-info .name"),jd:section?.querySelector(".job-sec-text")?.innerText?.trim()||null}}})())'
  });
  const publicFacts = typeof facts.value === "string" ? JSON.parse(facts.value) : facts.value;
  if (!publicFacts || !Array.isArray(publicFacts.cards)) throw new Error("INVALID_PUBLIC_FACTS");
  for (const card of publicFacts.cards) {
    card.url = trustedUrl(card.url);
  }
  return {url, accessibilityText: JSON.stringify({tree: snapshot.tree, cards: publicFacts.cards, detail: publicFacts.detail}), elementRefs: [], humanVerificationRequired: false};
}

if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) {
  try {
    let input = "";
    for await (const chunk of process.stdin) {
      input += chunk;
      if (input.length > 16384) throw new Error("INPUT_TOO_LARGE");
    }
    process.stdout.write(JSON.stringify(await execute(JSON.parse(input))));
  } catch {
    // 上游错误正文可能包含页面/账户信息，进程边界只发固定错误码。
    process.stderr.write("KIMI_BRIDGE_COMMAND_FAILED\n");
    process.exitCode = 1;
  }
}
