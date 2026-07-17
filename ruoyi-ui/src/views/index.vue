<template>
  <div class="dashboard-page">
    <div class="dashboard-hero">
      <div>
        <p class="dashboard-eyebrow">Overview Dashboard</p>
        <h1>渠道数据总览</h1>
        <p class="dashboard-subtitle">
          当前展示账号 {{ partnerStackKey || '-' }} 在 PartnerStack 的实时数据。
        </p>
      </div>
      <div class="hero-amount">
        <span>预估分销佣金</span>
        <strong>${{ formatMoney(summary.rewardAmount) }}</strong>
      </div>
    </div>

    <el-card shadow="never" class="filter-panel">
      <el-form :inline="true" :model="filters" class="dashboard-form" v-loading="loading">
        <el-form-item label="日期范围">
          <el-date-picker
            v-model="filters.dateRange"
            type="datetimerange"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            value-format="yyyy-MM-dd HH:mm:ss"
          />
        </el-form-item>
        <el-form-item label="SubId">
          <el-select v-model="filters.subId" placeholder="全部" clearable>
            <el-option
              v-for="item in subIdOptions"
              :key="item"
              :label="item"
              :value="item"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="交易ID">
          <el-input v-model="filters.transactionId" placeholder="请输入交易ID" clearable />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="el-icon-search" @click="handleSearch">查询</el-button>
          <el-button icon="el-icon-refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-row :gutter="16" class="metric-grid" v-loading="loading">
      <el-col v-for="card in metricCards" :key="card.key" :xs="24" :sm="12" :lg="8" :xl="6">
        <el-card shadow="hover" class="metric-card">
          <div class="metric-icon" :style="{ background: card.color }">
            <i :class="card.icon"></i>
          </div>
          <div class="metric-content">
            <span>{{ card.label }}</span>
            <strong>{{ card.value }}</strong>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="content-grid">
      <el-col :xs="24" :lg="16">
        <el-card shadow="never" class="detail-card">
          <div slot="header" class="card-header">
            <span>明细数据</span>
            <span class="header-tip">共 {{ tableData.length }} 条</span>
          </div>
          <el-table :data="tableData" stripe v-loading="loading" empty-text="当前条件下暂无 PartnerStack 数据">
            <el-table-column prop="customerKey" label="客户Key" min-width="180" />
            <el-table-column prop="subId" label="SUBID" min-width="120" />
            <el-table-column prop="signups" label="注册" align="right" />
            <el-table-column prop="paidSignups" label="付费注册" align="right" />
            <el-table-column prop="actions" label="动作数" align="right" />
            <el-table-column prop="validActions" label="有效动作数" align="right" />
            <el-table-column prop="transactions" label="交易笔数" align="right" />
            <el-table-column prop="transactionAmount" label="有效消耗" min-width="110" align="right">
              <template slot-scope="scope">${{ formatMoney(scope.row.transactionAmount) }}</template>
            </el-table-column>
            <el-table-column prop="rewardAmount" label="预估分销佣金" min-width="130" align="right">
              <template slot-scope="scope">${{ formatMoney(scope.row.rewardAmount) }}</template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>

      <el-col :xs="24" :lg="8">
        <el-card shadow="never" class="notice-card">
          <div slot="header" class="card-header">
            <span>结算说明</span>
          </div>
          <div class="notice-item">
            <i class="el-icon-date"></i>
            <span>业绩锁定日期：下个月 20 日（M+1）</span>
          </div>
          <div class="notice-item">
            <i class="el-icon-wallet"></i>
            <span>佣金结算日期：下下个月 20-30 日间（M+2）</span>
          </div>
          <div class="notice-divider"></div>
          <div class="notice-summary">
            <span>当前筛选条件下</span>
            <strong>{{ summary.transactions }} 笔交易</strong>
            <strong>{{ summary.signups }} 个注册动作</strong>
          </div>
        </el-card>

        <el-card shadow="never" class="mini-trend-card">
          <div slot="header" class="card-header">
            <span>筛选摘要</span>
          </div>
          <div class="trend-row">
            <span>注册动作 / 新增客户</span>
            <strong>{{ signupRate }}</strong>
          </div>
          <div class="trend-row">
            <span>付费动作 / 注册动作</span>
            <strong>{{ paidRate }}</strong>
          </div>
          <div class="trend-row">
            <span>付费客户占比</span>
            <strong>{{ paidCustomerRate }}</strong>
          </div>
          <div class="trend-row">
            <span>平均单次消耗</span>
            <strong>${{ averageTransactionAmount }}</strong>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import { getPartnerStackDashboard } from '@/api/partnerstack'
import { getUserProfile, updatePartnerStackKey } from '@/api/system/user'

const pad = value => `${value}`.padStart(2, '0')
const formatDateTime = date => `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
const createDefaultRange = () => {
  const end = new Date()
  const start = new Date(end.getTime() - 30 * 24 * 60 * 60 * 1000)
  return [formatDateTime(start), formatDateTime(end)]
}
const emptySummary = () => ({
  customers: 0,
  paidCustomers: 0,
  signups: 0,
  paidSignups: 0,
  actions: 0,
  validActions: 0,
  transactions: 0,
  transactionAmount: 0,
  rewards: 0,
  rewardAmount: 0
})

export default {
  name: 'Index',
  data() {
    return {
      loading: false,
      partnerStackKey: '',
      filters: {
        dateRange: createDefaultRange(),
        subId: '',
        transactionId: ''
      },
      subIdOptions: [],
      summary: emptySummary(),
      tableData: []
    }
  },
  computed: {
    metricCards() {
      return [
        { key: 'customers', label: '新增客户', value: this.summary.customers, icon: 'el-icon-user', color: 'linear-gradient(135deg, #6f7bf7, #8e66ff)' },
        { key: 'paidCustomers', label: '付费客户', value: this.summary.paidCustomers, icon: 'el-icon-s-custom', color: 'linear-gradient(135deg, #5f8bff, #6ca8ff)' },
        { key: 'signups', label: '注册动作', value: this.summary.signups, icon: 'el-icon-circle-plus-outline', color: 'linear-gradient(135deg, #23b7a4, #49d3a8)' },
        { key: 'actions', label: '动作数', value: this.summary.actions, icon: 'el-icon-data-analysis', color: 'linear-gradient(135deg, #f0b429, #f7d154)' },
        { key: 'validActions', label: '有效动作数', value: this.summary.validActions, icon: 'el-icon-histogram', color: 'linear-gradient(135deg, #4d7cff, #58c0ff)' },
        { key: 'transactions', label: '交易笔数', value: this.summary.transactions, icon: 'el-icon-tickets', color: 'linear-gradient(135deg, #f58a66, #f7b267)' },
        { key: 'transactionAmount', label: '有效消耗', value: `$${this.formatMoney(this.summary.transactionAmount)}`, icon: 'el-icon-wallet', color: 'linear-gradient(135deg, #8b5cf6, #c084fc)' },
        { key: 'rewards', label: '奖励笔数', value: this.summary.rewards, icon: 'el-icon-coin', color: 'linear-gradient(135deg, #17a2b8, #45c4d4)' }
      ]
    },
    signupRate() {
      return this.toPercent(this.summary.signups, this.summary.customers)
    },
    paidRate() {
      return this.toPercent(this.summary.paidSignups, this.summary.signups)
    },
    paidCustomerRate() {
      return this.toPercent(this.summary.paidCustomers, this.summary.customers)
    },
    averageTransactionAmount() {
      if (!this.summary.transactions) return '0.00'
      return this.formatMoney(Number(this.summary.transactionAmount) / this.summary.transactions)
    }
  },
  created() {
    this.initializeDashboard()
  },
  methods: {
    async initializeDashboard() {
      const response = await getUserProfile()
      const partnerStackKey = response.data && response.data.partnerStackKey
      if (partnerStackKey && partnerStackKey.trim()) {
        this.partnerStackKey = this.maskPartnerStackKey(partnerStackKey.trim())
        await this.fetchDashboard()
        return
      }
      this.promptPartnerStackKey()
    },
    promptPartnerStackKey() {
      this.$prompt('请输入当前账号的 PartnerStack API Token；也兼容合作关系Key（part_...）或客户Key（cus_...）。', '绑定 PartnerStack', {
        confirmButtonText: '立即绑定',
        cancelButtonText: '稍后绑定',
        closeOnClickModal: false,
        closeOnPressEscape: false,
        inputPlaceholder: '请输入 PartnerStack API Token',
        inputValidator(value) {
          const key = (value || '').trim()
          if (!key) return 'PartnerStack Key不能为空'
          if (key.length > 100) return 'PartnerStack Key长度不能超过100个字符'
          return true
        }
      }).then(async ({ value }) => {
        const partnerStackKey = value.trim()
        await updatePartnerStackKey(partnerStackKey)
        this.partnerStackKey = this.maskPartnerStackKey(partnerStackKey)
        this.$modal.msgSuccess('PartnerStack Key绑定成功')
        await this.fetchDashboard()
      }).catch(() => {})
    },
    async fetchDashboard() {
      this.loading = true
      try {
        const [start, end] = this.filters.dateRange || []
        const response = await getPartnerStackDashboard({
          minCreated: start ? new Date(start).getTime() : undefined,
          maxCreated: end ? new Date(end).getTime() : undefined,
          subId: this.filters.subId || undefined,
          transactionId: this.filters.transactionId || undefined
        })
        const data = response.data || {}
        this.partnerStackKey = data.partnerStackKey || this.partnerStackKey
        this.summary = { ...emptySummary(), ...(data.summary || {}) }
        this.tableData = data.rows || []
        this.subIdOptions = data.subIds || []
      } finally {
        this.loading = false
      }
    },
    handleSearch() {
      this.fetchDashboard()
    },
    handleReset() {
      this.filters = {
        dateRange: createDefaultRange(),
        subId: '',
        transactionId: ''
      }
      this.fetchDashboard()
    },
    formatMoney(value) {
      return Number(value || 0).toFixed(2)
    },
    maskPartnerStackKey(value) {
      if (!value || value.length <= 12 || value.startsWith('part_') || value.startsWith('cus_')) return value
      return `${value.slice(0, 4)}****${value.slice(-4)}`
    },
    toPercent(numerator, denominator) {
      if (!denominator) return '0.0%'
      return `${(Number(numerator || 0) * 100 / Number(denominator)).toFixed(1)}%`
    }
  }
}
</script>

<style lang="scss" scoped>
.dashboard-page {
  padding: 4px;
  background: #f5f7fb;
}

.dashboard-hero {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 20px;
  margin-bottom: 16px;
  padding: 28px 30px;
  color: #fff;
  border-radius: 20px;
  background: linear-gradient(135deg, #2f3f74 0%, #5868d6 55%, #8a63ff 100%);
  box-shadow: 0 18px 36px rgba(74, 92, 177, 0.22);
}

.dashboard-eyebrow {
  margin: 0 0 10px;
  font-size: 12px;
  letter-spacing: 0.14em;
  text-transform: uppercase;
  color: rgba(255, 255, 255, 0.72);
}

.dashboard-hero h1 {
  margin: 0;
  font-size: 32px;
  font-weight: 700;
}

.dashboard-subtitle {
  max-width: 560px;
  margin: 12px 0 0;
  font-size: 14px;
  line-height: 1.7;
  color: rgba(255, 255, 255, 0.84);
}

.hero-amount {
  min-width: 220px;
  padding: 18px 20px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.12);
  backdrop-filter: blur(8px);
}

.hero-amount span {
  display: block;
  margin-bottom: 10px;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.78);
}

.hero-amount strong {
  font-size: 38px;
  line-height: 1;
}

.filter-panel {
  margin-bottom: 16px;
  border: 0;
  border-radius: 18px;
}

.dashboard-form ::v-deep .el-form-item {
  margin-bottom: 0;
}

.dashboard-form ::v-deep .el-input__inner,
.dashboard-form ::v-deep .el-range-input,
.dashboard-form ::v-deep .el-date-editor {
  border-radius: 12px;
}

.metric-grid {
  margin-bottom: 16px;
}

.metric-card {
  display: flex;
  align-items: center;
  min-height: 124px;
  border: 0;
  border-radius: 18px;
  box-shadow: 0 10px 26px rgba(25, 39, 75, 0.06);
}

.metric-card ::v-deep .el-card__body {
  display: flex;
  align-items: center;
  width: 100%;
}

.metric-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 52px;
  height: 52px;
  margin-right: 16px;
  color: #fff;
  border-radius: 16px;
  font-size: 22px;
  flex-shrink: 0;
}

.metric-content span {
  display: block;
  margin-bottom: 8px;
  font-size: 13px;
  color: #7d879c;
}

.metric-content strong {
  font-size: 34px;
  line-height: 1;
  color: #1f2940;
}

.content-grid {
  margin-bottom: 10px;
}

.detail-card,
.notice-card,
.mini-trend-card {
  border: 0;
  border-radius: 18px;
  box-shadow: 0 10px 26px rgba(25, 39, 75, 0.06);
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-weight: 600;
  color: #1f2940;
}

.header-tip {
  font-size: 12px;
  font-weight: 400;
  color: #8b94a7;
}

.notice-card {
  margin-bottom: 16px;
}

.notice-item {
  display: flex;
  align-items: center;
  margin-bottom: 14px;
  font-size: 13px;
  color: #475069;
}

.notice-item i {
  margin-right: 10px;
  color: #5d7cff;
  font-size: 16px;
}

.notice-divider {
  height: 1px;
  margin: 18px 0;
  background: #e9edf5;
}

.notice-summary {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.notice-summary span {
  color: #8b94a7;
  font-size: 12px;
}

.notice-summary strong {
  color: #1f2940;
  font-size: 22px;
}

.mini-trend-card .trend-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 0;
  border-bottom: 1px solid #eef2f8;
  font-size: 13px;
  color: #657089;
}

.mini-trend-card .trend-row:last-child {
  padding-bottom: 0;
  border-bottom: 0;
}

.mini-trend-card strong {
  color: #1f2940;
  font-size: 18px;
}

@media (max-width: 992px) {
  .dashboard-hero {
    flex-direction: column;
  }

  .hero-amount {
    min-width: auto;
    width: 100%;
  }
}
</style>
