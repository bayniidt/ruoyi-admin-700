# 下级代理消耗上报接口

每个代理在后台“代理管理 → 新建代理”时获得一组独立凭证。`API Secret` 只显示一次；遗失后请在后台重置，旧 Secret 会立即失效。

## 请求

```http
POST /openapi/v1/agent/usage
Content-Type: application/json
X-Agent-Key: ag_xxxxxxxxx
X-Agent-Secret: xxxxxxxxx
```

```json
{
  "requestId": "usage_20260716_0001",
  "metric": "ad_spend",
  "dataCount": 1000,
  "spend": 128.5,
  "currency": "USD",
  "customerKey": "customer_001",
  "reportedAt": "2026-07-16T17:00:00+08:00",
  "remark": "当日消耗"
}
```

字段说明：

- `requestId`：必填，调用方生成的唯一请求号；同一代理重复提交不会重复计入。
- `metric`：必填，消耗类型，例如 `ad_spend`、`api_call`。
- `dataCount`：可选，本次消耗的数据量，默认 `0`。
- `spend`：可选，本次消耗金额，默认 `0`。
- `currency`：可选，默认 `USD`。
- `customerKey`：可选，下级系统内的客户标识，用于进一步拆分统计。
- `reportedAt`：可选，业务发生时间；不传时使用服务端接收时间。
- `remark`：可选，备注。

成功响应：

```json
{
  "code": 200,
  "msg": "消耗数据上报成功",
  "data": {
    "requestId": "usage_20260716_0001",
    "duplicate": false
  }
}
```

凭证无效时响应体 `code` 为 `401`。建议调用方仅在服务端保存凭证，不要放进网页或 App 安装包。
