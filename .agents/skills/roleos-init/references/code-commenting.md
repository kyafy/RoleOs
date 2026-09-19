# RoleOS 代码注释规范

注释的目标是保存代码本身无法可靠表达的契约和决策，而不是提高注释数量。默认使用中文说明业务
语义，代码标识符、协议字段和框架术语保留英文；对外英文 SDK 或协议按其既有语言保持一致。

## 必须说明的内容

以下位置必须使用 Javadoc 或与文件格式匹配的结构化注释：

- 模块根包、领域边界包和 Adapter 边界包：使用 `package-info.java` 说明职责、公开入口、允许依赖、
  禁止依赖以及事实/副作用边界；
- 公共 Port、Use Case、领域服务和跨模块 DTO：说明输入输出语义、前置条件、幂等性、失败方式、
  事务或一致性边界；
- Workflow 状态、Transition、Retry、Approval：说明合法来源/目标、守卫条件、重试语义和需要人工
  确认的副作用；
- ErrorCode 和异常：说明稳定语义、触发条件及是否可重试；不得把 HTTP 状态写入领域异常；
- `@ConfigurationProperties`：说明单位、范围、默认值、是否敏感、生产是否必填以及重启是否生效；
- 外部 Adapter：说明上游约束、超时/重试/幂等策略、数据最小化和失败转换；
- 安全、权限、路径、Secret、反序列化、外部 URL 和命令执行逻辑：说明威胁边界以及为何采用当前
  校验方式；
- 不明显的算法、性能折中和兼容性 workaround：说明“为什么”，并关联可追踪的 issue/Task；
- Flyway 迁移：说明迁移意图、数据兼容策略、锁表/重写风险、失败后的恢复方式；
- Shell/Docker/Compose：说明非显然环境变量、持久化目录、退出码和可能产生状态变更的操作。

## Javadoc 写法

- 首句给出契约结论，不写“这是一个……”之类空话。
- 说明业务语义和不变量，不复述类名、方法名或 Java 语法。
- 对公共方法记录有意义的 `@param`、`@return`、`@throws`；不存在返回值或异常时不要制造标签。
- 需要区分调用方须知与实现约束时使用 `@apiNote`、`@implSpec` 或 `@implNote`。
- override 仅在收紧约束、增加副作用或解释实现差异时补充；完全继承契约时不复制父类文本。
- 示例只用于容易误用的协议、状态转换或边界值；示例必须可编译或由测试覆盖，不能长期漂移。
- 注释随代码同批更新。若注释和可执行规则冲突，以测试/代码为事实并立即修正注释。

### 类与接口模板

模板只展示信息结构，删除不适用的段落和标签，禁止原样复制空话：

```java
/**
 * 持久化并推进 Career Workflow 的确定性状态转换。
 *
 * <p>只接受满足当前 Stage 守卫条件的 Decision；Agent 输出不能直接修改 Workflow State。
 * 所有成功转换必须与 WorkflowEvent 在同一事务提交。
 *
 * @apiNote 调用方必须提供稳定的 commandId 以保证重试幂等。
 * @implSpec 实现不得调用浏览器、模型或其他不可回滚的外部副作用。
 */
public interface WorkflowTransitionService {}
```

类/接口注释按需包含：一句话职责、所在边界、关键不变量、线程安全/事务语义、副作用、扩展限制。
不能只写“WorkflowTransitionService 接口”或重复类名。

### 方法模板

```java
/**
 * 在当前状态允许时应用一次幂等转换，并返回持久化后的 Workflow 快照。
 *
 * @param workflowId 待转换的 Workflow 标识，不能为空
 * @param commandId 幂等命令标识；相同值重试不得产生第二次转换
 * @param decision 已通过 Schema 校验的 Agent Decision
 * @return 已持久化的新状态快照
 * @throws WorkflowConflictException 当前 Stage 不接受该 Decision 时抛出
 */
WorkflowSnapshot transition(WorkflowId workflowId, CommandId commandId, AgentDecision decision);
```

方法注释重点描述调用方无法从签名看出的语义：单位、边界值、幂等、顺序、事务、阻塞行为、
副作用、失败分类和返回值状态。private 方法仅在算法或约束不明显时注释。

## 作者、日期与版本

- 源文件和 Javadoc 默认**不写** `@author`、个人姓名、邮箱、`@date`、创建时间、修改时间或修改历史；
  Git blame/log 才是可审计且不会漂移的事实来源。
- 不生成 IDE 风格的个人文件头。版权/许可证头仅在仓库法律政策明确要求时统一生成。
- `@since` 只用于稳定、对外发布且有版本语义的公共 API，值使用项目发布版本，例如 `@since 1.2`；
  内部类、普通业务方法和尚未发布的骨架不写 `@since`。
- Deprecated API 必须使用 `@Deprecated` 与 `@deprecated`，说明替代项和移除条件；不得只写日期。
- 运行日志的事件时间由日志框架统一生成，代码中不得手工拼接时间，也不记录代码作者。

## 不应添加的注释

- `// 设置名称`、`// 循环列表`、`// 返回结果` 等逐行翻译；
- 为 getter、setter、record accessor、显然的构造器或简单委托生成模板 Javadoc；
- 注释掉的旧代码、过期实现、复制的需求全文或可以由 Git 历史找到的变更记录；
- 猜测未来设计的长篇描述；未批准的设计应进入 Spec/Plan，而不是写进代码；
- 密码、Token、Cookie、真实简历/JD、个人信息、内部地址或生产数据样例；
- 用 `TODO`、`FIXME` 或“后续实现”代替当前 Task 的核心行为。

复杂架构决策应写入 Spec、Plan 或 ADR；代码中只保留一句决策摘要和文档链接，避免形成第二份事实源。

## TODO 与临时措施

仅允许不影响当前验收且有明确跟踪项的临时工作：

```text
TODO(T123): 移除兼容分支；上游 OryxOS 版本支持结构化错误后执行。
FIXME(T456): 当前超时映射会丢失上游分类，替换前保持 UNKNOWN 语义。
```

- 必须包含 Spec Kit Task ID 或 issue ID、退出条件和必要原因；
- 禁止无标识的 `TODO`/`FIXME`，禁止写个人姓名作为唯一负责人；
- 当前 Task 的 Done 条件、真实性、安全或人工审批要求不得延期为 TODO。

## 非 Java 文件

- YAML/Properties：在配置组前说明用途；关键键说明单位、默认行为、环境变量和生产约束，不给每个
  显然键重复写注释。
- SQL/Flyway：注释不可逆操作、数据回填、约束建立顺序和大表风险；不要放真实数据。
- Shell：优先拆成有名称的函数；注释危险或非显然操作，命令本身已清楚时不重复解释。
- 测试：方法名优先表达场景；只有复杂 Fixture、时间线、并发或 Golden Scenario 才使用
  Given/When/Then 注释，不给每个断言配注释。

## 自动化门禁

初始化时建立与现有代码成熟度匹配的渐进门禁：

1. Checkstyle 检查公共/受保护类型和公共契约的 Javadoc，排除生成代码、测试 Fixture、简单 override；
2. `maven-javadoc-plugin` + doclint 验证文档可生成且链接/标签有效；
3. 扫描无 Task/issue ID 的 `TODO|FIXME`、连续注释掉的 Java 代码和注释中的 Secret 模式；
4. 扫描 `@author`、`@date`、个人文件头和手写修改历史；许可证策略要求的统一文件头单独豁免；
5. ArchUnit 或模块测试验证 `package-info.java` 描述的关键依赖方向没有被代码破坏；
6. Review 检查注释是否解释“为什么/约束”，自动工具不使用注释行覆盖率作为质量指标。

新增门禁不能一次性把遗留仓库全部打红。先阻止新增违规，再为存量问题建立显式、可缩减的基线；
不得以永久 suppress 或全局关闭 doclint 代替修复。
