# 奖励明细过滤动作奖励实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在前端过滤 PartnerStack 的动作奖励，并让奖励表格与顶部统计使用相同的过滤后数据。

**Architecture:** 新增一个无副作用的奖励过滤函数，以原始字段 `source.type` 判断是否为动作奖励。奖励明细页面在映射表格字段和计算汇总前调用该函数，确保两处数据口径一致。

**Tech Stack:** Vue 2、JavaScript、Node.js 内置 `assert`

## Global Constraints

- 仅修改前端。
- 不修改 PartnerStack 接口、后端或数据库。
- 只过滤 `source.type === 'action'`，其他来源保留。

---

### Task 1: 过滤动作奖励并统一统计口径

**Files:**
- Create: `ruoyi-ui/src/views/incentive/rewardFilter.js`
- Create: `ruoyi-ui/src/views/incentive/rewardFilter.test.js`
- Modify: `ruoyi-ui/src/views/incentive/index.vue:150-168`

**Interfaces:**
- Consumes: PartnerStack 奖励对象数组。
- Produces: `filterVisibleRewards(items)`，返回不包含动作奖励的新数组。

- [ ] **Step 1: 写失败测试**

```js
const assert = require('assert')
const { filterVisibleRewards } = require('./rewardFilter')

const rewards = [
  { key: 'transaction', source: { type: 'transaction' }, amount: 1000 },
  { key: 'action', source: { type: 'action' }, amount: 4000 },
  { key: 'unknown', source: null, amount: 2000 }
]

assert.deepStrictEqual(
  filterVisibleRewards(rewards).map(item => item.key),
  ['transaction', 'unknown']
)
assert.strictEqual(rewards.length, 3)
```

- [ ] **Step 2: 运行测试并确认失败**

Run: `node ruoyi-ui/src/views/incentive/rewardFilter.test.js`

Expected: FAIL，因为 `rewardFilter` 模块尚不存在。

- [ ] **Step 3: 添加最小过滤实现**

```js
function filterVisibleRewards(items) {
  return (Array.isArray(items) ? items : [])
    .filter(item => item?.source?.type !== 'action')
}

module.exports = { filterVisibleRewards }
```

- [ ] **Step 4: 接入奖励明细页面**

在 `index.vue` 中导入 `filterVisibleRewards`，将接口提取结果改为：

```js
const list = filterVisibleRewards(this.extractItems(data))
```

现有表格映射和汇总计算继续共同使用 `list`。

- [ ] **Step 5: 验证测试与生产构建**

Run:

```bash
node ruoyi-ui/src/views/incentive/rewardFilter.test.js
cd ruoyi-ui && npm run build:prod
```

Expected: 测试退出码为 0，生产构建成功。

- [ ] **Step 6: 检查最终差异**

Run:

```bash
git diff --check
git diff -- ruoyi-ui/src/views/incentive/index.vue ruoyi-ui/src/views/incentive/rewardFilter.js ruoyi-ui/src/views/incentive/rewardFilter.test.js
```

Expected: 无空白错误；差异仅包含前端过滤及其测试。
