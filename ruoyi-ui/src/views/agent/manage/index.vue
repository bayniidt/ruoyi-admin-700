<template>
  <div class="app-container agent-manage-page">
    <el-card shadow="never" class="page-card">
      <div class="toolbar">
        <el-button type="primary" icon="el-icon-plus" @click="handleAdd">新建代理</el-button>
        <div class="toolbar-search">
          <el-input
            v-model="queryParams.keyword"
            placeholder="用户名"
            clearable
            @keyup.enter.native="handleQuery"
          >
            <el-button slot="append" icon="el-icon-search" @click="handleQuery" />
          </el-input>
        </div>
      </div>

      <el-table :data="agentList" border class="agent-table" v-loading="loading">
        <el-table-column prop="userName" label="用户名" min-width="160" />
        <el-table-column prop="nickName" label="昵称" min-width="160" />
        <el-table-column prop="remark" label="备注" min-width="180">
          <template slot-scope="scope">
            <span>{{ scope.row.remark || "-" }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="commissionRate" label="分成比例" min-width="140" align="center">
          <template slot-scope="scope">
            <span>{{ scope.row.commissionRate }}%</span>
          </template>
        </el-table-column>
        <el-table-column prop="apiKey" label="接口 Key" min-width="230">
          <template slot-scope="scope">
            <span class="api-key">{{ scope.row.apiKey }}</span>
            <el-button type="text" size="mini" @click="copyText(scope.row.apiKey)">复制</el-button>
          </template>
        </el-table-column>
        <el-table-column prop="dataCount" label="消耗数据量" min-width="120" align="right" />
        <el-table-column prop="totalSpend" label="消耗金额" min-width="120" align="right">
          <template slot-scope="scope">${{ formatMoney(scope.row.totalSpend) }}</template>
        </el-table-column>
        <el-table-column prop="lastReportTime" label="最后上报" min-width="180" align="center">
          <template slot-scope="scope">{{ scope.row.lastReportTime || '-' }}</template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" min-width="200" align="center" />
        <el-table-column label="操作" width="100" fixed="right" align="center">
          <template slot-scope="scope">
            <el-button type="text" @click="handleResetSecret(scope.row)">重置密钥</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="table-footer">
        <div class="table-total">共 {{ total }} 条</div>
        <el-pagination
          background
          layout="sizes, prev, pager, next"
          :current-page.sync="queryParams.pageNum"
          :page-size.sync="queryParams.pageSize"
          :page-sizes="[20, 30, 50]"
          :total="total"
          @current-change="handleCurrentChange"
          @size-change="handleSizeChange"
        />
      </div>
    </el-card>

    <el-dialog title="新建代理" :visible.sync="open" width="600px" append-to-body @close="handleCancel">
      <el-form ref="form" :model="form" :rules="rules" label-width="96px" class="agent-form">
        <el-form-item label="用户名" prop="userName">
          <el-input v-model="form.userName" placeholder="请输入用户名" maxlength="20" />
        </el-form-item>

        <el-form-item label="用户密码" prop="password">
          <el-input v-model="form.password" type="password" placeholder="请输入用户密码" show-password />
        </el-form-item>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="手机号码" prop="phonenumber">
              <el-input v-model="form.phonenumber" placeholder="请输入手机号码" maxlength="11" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="邮箱" prop="email">
              <el-input v-model="form.email" placeholder="请输入邮箱" maxlength="50" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="用户昵称" prop="nickName">
          <el-input v-model="form.nickName" placeholder="请输入用户昵称" maxlength="20" />
        </el-form-item>

        <el-form-item label="客户Key" prop="partnerCustomerKey">
          <el-input v-model="form.partnerCustomerKey" placeholder="PartnerStack 客户Key；不填默认使用用户名" maxlength="100" />
        </el-form-item>

        <el-form-item label="佣金比例" prop="commissionRate">
          <div class="commission-field">
            <el-input-number
              v-model="form.commissionRate"
              controls-position="right"
              :min="0"
              :max="maxCommissionRate"
            />
            <span class="commission-tip">最大佣金比例：{{ maxCommissionRate }}%</span>
          </div>
        </el-form-item>

        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" :rows="3" placeholder="请输入内容" maxlength="200" show-word-limit />
        </el-form-item>
      </el-form>

      <div slot="footer" class="dialog-footer">
        <el-button @click="handleCancel">取 消</el-button>
        <el-button type="primary" @click="submitForm">确 定</el-button>
      </div>
    </el-dialog>

    <el-dialog title="接口凭证（仅显示这一次）" :visible.sync="credentialOpen" width="680px" append-to-body>
      <el-alert
        title="请立即复制并通过安全方式交给下级客户。系统不会保存明文 Secret，关闭后无法再次查看。"
        type="warning"
        :closable="false"
        show-icon
      />
      <el-descriptions :column="1" border class="credential-box">
        <el-descriptions-item label="API 地址">{{ usageApiUrl }}</el-descriptions-item>
        <el-descriptions-item label="X-Agent-Key">
          <code>{{ credential.apiKey }}</code>
          <el-button type="text" @click="copyText(credential.apiKey)">复制</el-button>
        </el-descriptions-item>
        <el-descriptions-item label="X-Agent-Secret">
          <code>{{ credential.apiSecret }}</code>
          <el-button type="text" @click="copyText(credential.apiSecret)">复制</el-button>
        </el-descriptions-item>
      </el-descriptions>
      <div class="payload-title">上报示例（POST JSON）</div>
      <pre class="payload-example">{{ payloadExample }}</pre>
      <div slot="footer">
        <el-button type="primary" @click="credentialOpen = false">我已保存</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { addAgent, listAgents, resetAgentSecret } from '@/api/agent'

function createDefaultForm() {
  return {
    userName: '',
    password: '',
    phonenumber: '',
    email: '',
    nickName: '',
    partnerCustomerKey: '',
    commissionRate: 0,
    remark: ''
  }
}

export default {
  name: 'AgentManage',
  data() {
    return {
      open: false,
      credentialOpen: false,
      loading: false,
      total: 0,
      credential: { apiKey: '', apiSecret: '' },
      maxCommissionRate: 20,
      queryParams: {
        keyword: '',
        pageNum: 1,
        pageSize: 20
      },
      agentList: [],
      form: createDefaultForm(),
      rules: {
        userName: [
          { required: true, message: '用户名不能为空', trigger: 'blur' },
          { min: 2, max: 20, message: '用户名长度必须介于 2 和 20 之间', trigger: 'blur' }
        ],
        password: [
          { required: true, message: '用户密码不能为空', trigger: 'blur' }
        ],
        nickName: [
          { required: true, message: '用户昵称不能为空', trigger: 'blur' }
        ],
        email: [
          {
            type: 'email',
            message: '请输入正确的邮箱地址',
            trigger: ['blur', 'change']
          }
        ],
        phonenumber: [
          {
            pattern: /^1[3-9]\d{9}$/,
            message: '请输入正确的手机号码',
            trigger: 'blur'
          }
        ],
        commissionRate: [
          { required: true, message: '佣金比例不能为空', trigger: 'change' }
        ]
      }
    }
  },
  computed: {
    usageApiUrl() {
      return `${window.location.origin}${process.env.VUE_APP_BASE_API}/openapi/v1/agent/usage`
    },
    payloadExample() {
      return JSON.stringify({
        requestId: `usage_${Date.now()}`,
        metric: 'ad_spend',
        dataCount: 1000,
        spend: 128.5,
        currency: 'USD',
        customerKey: 'customer_001',
        reportedAt: new Date().toISOString(),
        remark: '当日消耗'
      }, null, 2)
    }
  },
  created() {
    this.getList()
  },
  methods: {
    async getList() {
      this.loading = true
      try {
        const response = await listAgents(this.queryParams)
        this.agentList = response.rows || []
        this.total = Number(response.total || 0)
      } finally {
        this.loading = false
      }
    },
    handleQuery() {
      this.queryParams.pageNum = 1
      this.getList()
    },
    handleCurrentChange(page) {
      this.queryParams.pageNum = page
      this.getList()
    },
    handleSizeChange(size) {
      this.queryParams.pageSize = size
      this.queryParams.pageNum = 1
      this.getList()
    },
    handleAdd() {
      this.form = createDefaultForm()
      this.open = true
      this.$nextTick(() => {
        this.$refs.form && this.$refs.form.clearValidate()
      })
    },
    handleCancel() {
      this.open = false
      this.form = createDefaultForm()
    },
    submitForm() {
      this.$refs.form.validate(valid => {
        if (!valid) {
          return
        }
        addAgent(this.form).then(response => {
          this.open = false
          this.queryParams.pageNum = 1
          this.credential = response.data
          this.credentialOpen = true
          this.getList()
        })
      })
    },
    handleResetSecret(row) {
      this.$confirm(`重置后，代理 ${row.userName} 的旧 Secret 会立即失效，是否继续？`, '重置接口密钥', {
        type: 'warning'
      }).then(() => resetAgentSecret(row.agentId)).then(response => {
        this.credential = { apiKey: row.apiKey, apiSecret: response.data.apiSecret }
        this.credentialOpen = true
      }).catch(() => {})
    },
    copyText(value) {
      if (!value) return
      navigator.clipboard.writeText(value).then(() => this.$message.success('已复制'))
    },
    formatMoney(value) {
      return Number(value || 0).toFixed(2)
    }
  }
}
</script>

<style lang="scss" scoped>
.agent-manage-page {
  .page-card {
    border-radius: 18px;
  }

  .toolbar {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 16px;
    margin-bottom: 18px;
  }

  .toolbar-search {
    width: 260px;
    max-width: 100%;
  }

  .agent-table ::v-deep .el-table__header th {
    background: #f7f9fc;
    color: #5d6785;
    font-weight: 600;
  }

  .table-footer {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 16px;
    padding-top: 18px;
  }

  .table-total {
    color: #7b849d;
    font-size: 13px;
  }

  .agent-form {
    padding-right: 12px;
  }

  .commission-field {
    display: flex;
    align-items: center;
    gap: 12px;
    flex-wrap: wrap;
  }

  .commission-tip {
    color: #f56c6c;
    font-size: 13px;
  }

  .api-key {
    margin-right: 8px;
    font-family: monospace;
  }

  .credential-box {
    margin-top: 18px;
  }

  .payload-title {
    margin: 18px 0 8px;
    color: #606266;
    font-weight: 600;
  }

  .payload-example {
    padding: 14px;
    overflow: auto;
    color: #d6deeb;
    background: #1f2937;
    border-radius: 6px;
  }
}
</style>
