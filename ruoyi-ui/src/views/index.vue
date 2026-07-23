<template>
  <div class="dashboard-page">
    <section class="filter-panel">
      <el-form :inline="true" :model="filters" class="dashboard-form">
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
            <el-option v-for="item in subIdOptions" :key="item" :label="item" :value="item" />
          </el-select>
        </el-form-item>
        <el-form-item label="交易ID">
          <el-input v-model="filters.transactionId" placeholder="模糊搜索" clearable />
        </el-form-item>
        <el-form-item class="query-item">
          <el-button type="primary" icon="el-icon-search" :loading="loading" @click="handleSearch">查询</el-button>
        </el-form-item>
      </el-form>
    </section>

    <section class="metric-grid">
      <article v-for="card in metricCards" :key="card.key" class="metric-card">
        <span class="metric-icon" :class="card.tone"><i :class="card.icon" /></span>
        <span class="metric-copy">
          <span>{{ card.label }}</span>
          <strong>{{ card.value }}</strong>
        </span>
      </article>
    </section>

    <section class="commission-bar">
      <span class="commission-label">
        <i class="el-icon-s-data" />
        预估分销佣金
        <i class="el-icon-question commission-help" />
      </span>
      <strong>${{ formatMoney(summary.rewardAmount) }}</strong>
    </section>

    <section class="settlement-tip">
      <i class="el-icon-info" />
      <span>业绩锁定日期: 下个月20日（M+1） | 佣金结算日期: 下下个月20-30日间（M+2）</span>
    </section>

    <section class="detail-panel">
      <header class="detail-header">
        <strong>明细数据</strong>
        <span v-if="loadingStage" class="loading-stage"><i class="el-icon-loading" /> {{ loadingStage }}</span>
        <span v-else>共 {{ tableData.length }} 条</span>
      </header>
      <div class="table-wrap">
        <el-table
          :data="tableData"
          empty-text="暂无数据"
          border
        >
          <el-table-column prop="subId" label="SUBID" min-width="150">
            <template slot-scope="scope"><span class="subid-link">{{ scope.row.subId || '-' }}</span></template>
          </el-table-column>
          <el-table-column prop="rawClicks" label="总点击" min-width="145" align="right">
            <template slot-scope="scope"><span class="metric-value">{{ scope.row.rawClicks || 0 }}</span></template>
          </el-table-column>
          <el-table-column prop="uniqueClicks" label="不重复点击" min-width="145" align="right">
            <template slot-scope="scope"><span class="metric-value">{{ scope.row.uniqueClicks || 0 }}</span></template>
          </el-table-column>
          <el-table-column prop="signups" label="注册" min-width="145" align="right">
            <template slot-scope="scope"><span class="metric-value">{{ scope.row.signups || 0 }}</span></template>
          </el-table-column>
          <el-table-column prop="paidSignups" label="付费注册" min-width="145" align="right">
            <template slot-scope="scope"><span class="metric-value">{{ scope.row.paidSignups || 0 }}</span></template>
          </el-table-column>
          <el-table-column prop="actions" label="动作数" min-width="145" align="right">
            <template slot-scope="scope"><span class="metric-value">{{ scope.row.actions || 0 }}</span></template>
          </el-table-column>
          <el-table-column prop="validActions" label="有效动作数" min-width="145" align="right">
            <template slot-scope="scope"><span class="metric-value">{{ scope.row.validActions || 0 }}</span></template>
          </el-table-column>
          <el-table-column prop="transactionAmount" label="有效消耗" min-width="145" align="right">
            <template slot-scope="scope"><span class="metric-value">${{ formatMoney(scope.row.transactionAmount) }}</span></template>
          </el-table-column>
          <el-table-column prop="rewardAmount" label="预估分销佣金" min-width="145" align="right">
            <template slot-scope="scope"><span class="metric-value">${{ formatMoney(scope.row.rewardAmount) }}</span></template>
          </el-table-column>
        </el-table>
      </div>
    </section>
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
  rawClicks: 0,
  uniqueClicks: 0,
  signups: 0,
  paidSignups: 0,
  actions: 0,
  validActions: 0,
  transactionAmount: 0,
  rewardAmount: 0
})
const dashboardStages = [
  { source: 'customers', label: '正在加载广告户...' },
  { source: 'actions', label: '正在加载动作数据...' },
  { source: 'transactions', label: '正在加载消耗数据...' },
  { source: 'rewards', label: '正在加载奖励数据...' }
]
const sourceFields = {
  customers: ['customers', 'paidCustomers'],
  actions: ['signups', 'paidSignups', 'actions', 'validActions'],
  transactions: ['transactions', 'transactionAmount'],
  rewards: ['rewards', 'rewardAmount']
}

export default {
  name: 'Index',
  data() {
    return {
      loading: false,
      loadingStage: '',
      dashboardRequestId: 0,
      dashboardNeedsRefresh: false,
      partnerStackKey: '',
      filters: { dateRange: createDefaultRange(), subId: '', transactionId: '' },
      subIdOptions: [],
      summary: emptySummary(),
      tableData: []
    }
  },
  computed: {
    metricCards() {
      return [
        { key: 'rawClicks', label: '原始点击', value: this.summary.rawClicks || 0, icon: 'el-icon-view', tone: 'purple' },
        { key: 'uniqueClicks', label: '不重复点击', value: this.summary.uniqueClicks || 0, icon: 'el-icon-thumb', tone: 'blue' },
        { key: 'signups', label: '注册数', value: this.summary.signups || 0, icon: 'el-icon-s-custom', tone: 'blue' },
        { key: 'paidSignups', label: '付费注册数', value: this.summary.paidSignups || 0, icon: 'el-icon-user', tone: 'blue' },
        { key: 'actions', label: '动作数', value: this.summary.actions || 0, icon: 'el-icon-s-data', tone: 'amber' },
        { key: 'validActions', label: '有效动作数', value: this.summary.validActions || 0, icon: 'el-icon-s-data', tone: 'blue' },
        { key: 'transactionAmount', label: '有效消耗', value: `$${this.formatMoney(this.summary.transactionAmount)}`, icon: 'el-icon-wallet', tone: 'purple' }
      ]
    }
  },
  created() {
    this.initializeDashboard()
  },
  beforeDestroy() {
    this.dashboardRequestId += 1
  },
  activated() {
    if (this.dashboardNeedsRefresh && this.partnerStackKey) {
      this.dashboardNeedsRefresh = false
      this.fetchDashboard()
    }
  },
  deactivated() {
    this.dashboardNeedsRefresh = this.dashboardNeedsRefresh || this.loading
    this.dashboardRequestId += 1
    this.loading = false
    this.loadingStage = ''
  },
  methods: {
    async initializeDashboard() {
      const response = await getUserProfile()
      const key = response.data && response.data.partnerStackKey
      if (key && key.trim()) {
        this.partnerStackKey = this.maskPartnerStackKey(key.trim())
        await this.fetchDashboard()
        return
      }
      this.promptPartnerStackKey()
    },
    promptPartnerStackKey() {
      this.$prompt('PartnerStack Key 用于查询当前账号自己的 PartnerStack 数据，请先完成绑定。', '绑定 PartnerStack Key', {
        confirmButtonText: '立即绑定',
        cancelButtonText: '稍后绑定',
        closeOnClickModal: false,
        closeOnPressEscape: false,
        inputPlaceholder: '请输入 PartnerStack API Token 或数据 Key',
        inputValidator(value) {
          const key = (value || '').trim()
          if (!key) return 'PartnerStack Key不能为空'
          if (key.length > 100) return 'PartnerStack Key长度不能超过100个字符'
          return true
        }
      }).then(async ({ value }) => {
        const key = value.trim()
        await updatePartnerStackKey(key)
        this.partnerStackKey = this.maskPartnerStackKey(key)
        this.$modal.msgSuccess('PartnerStack Key绑定成功')
        await this.fetchDashboard()
      }).catch(() => {})
    },
    async fetchDashboard() {
      const requestId = ++this.dashboardRequestId
      this.loading = true
      this.loadingStage = ''
      this.summary = emptySummary()
      this.tableData = []
      const rowMap = new Map()
      try {
        const [start, end] = this.filters.dateRange || []
        const params = {
          minCreated: start ? new Date(start).getTime() : undefined,
          maxCreated: end ? new Date(end).getTime() : undefined,
          subId: this.filters.subId || undefined,
          transactionId: this.filters.transactionId || undefined
        }
        this.loadingStage = '正在加载汇总数据...'
        const response = await getPartnerStackDashboard(params)
        if (requestId !== this.dashboardRequestId || this._isDestroyed) return
        for (const stage of dashboardStages) {
          this.loadingStage = stage.label
          this.mergeDashboardSource(response.data || {}, stage.source, rowMap)
        }
      } finally {
        if (requestId === this.dashboardRequestId) {
          this.loading = false
          this.loadingStage = ''
        }
      }
    },
    mergeDashboardSource(data, source, rowMap) {
      this.partnerStackKey = data.partnerStackKey || this.partnerStackKey
      const nextSummary = { ...this.summary }
      for (const field of sourceFields[source] || []) {
        nextSummary[field] = (data.summary || {})[field] || 0
      }
      this.summary = nextSummary

      for (const sourceRow of data.rows || []) {
        const subId = sourceRow.subId || '-'
        const row = rowMap.get(subId) || { subId, ...emptySummary() }
        for (const field of sourceFields[source] || []) {
          row[field] = sourceRow[field] || 0
        }
        rowMap.set(subId, row)
      }
      this.tableData = Array.from(rowMap.values())

      const subIds = [...new Set([...this.subIdOptions, ...(data.subIds || [])])]
      this.subIdOptions = subIds
    },
    handleSearch() {
      this.fetchDashboard()
    },
    formatMoney(value) {
      return Number(value || 0).toFixed(2)
    },
    maskPartnerStackKey(value) {
      if (!value || value.length <= 12 || value.startsWith('part_') || value.startsWith('cus_')) return value
      return `${value.slice(0, 4)}****${value.slice(-4)}`
    }
  }
}
</script>

<style lang="scss" scoped>
.dashboard-page {
  min-height: calc(100vh - 104px);
  padding: 20px;
  background: #f3f6f9;
  color: #1f2d3d;
}

.filter-panel {
  min-height: 92px;
  margin-bottom: 20px;
  padding: 20px;
  background: #fff;
  border: 1px solid #edf0f5;
  border-radius: 4px;
}

.dashboard-form {
  display: flex;
  align-items: center;
  min-height: 50px;
}

.dashboard-form ::v-deep .el-form-item {
  display: flex;
  align-items: center;
  margin: 0 28px 0 0;
}

.dashboard-form ::v-deep .el-form-item__label {
  padding-right: 12px;
  color: #606a7b;
  font-weight: 600;
}

.dashboard-form ::v-deep .el-date-editor {
  width: 400px;
}

.dashboard-form ::v-deep .el-select,
.dashboard-form ::v-deep .el-input {
  width: 190px;
}

.dashboard-form .query-item {
  margin-right: 0;
}

.dashboard-form ::v-deep .el-button {
  min-width: 88px;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px 20px;
  margin-bottom: 12px;
}

.metric-card {
  display: flex;
  align-items: center;
  min-height: 122px;
  padding: 28px 38px;
  background: #fff;
  border: 1px solid #edf0f5;
  border-radius: 4px;
  box-shadow: 0 2px 6px rgba(31, 45, 61, 0.03);
}

.metric-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 48px;
  height: 48px;
  margin-right: 14px;
  color: #fff;
  border-radius: 7px;
  font-size: 20px;
  flex: 0 0 auto;
}

.metric-icon.blue { background: #3d7eff; }
.metric-icon.purple { background: #805df0; }
.metric-icon.amber { background: #eda42b; }

.metric-copy > span {
  display: block;
  margin-bottom: 7px;
  color: #8c98aa;
  font-size: 13px;
}

.metric-copy strong {
  color: #182238;
  font-size: 23px;
  line-height: 1;
}

.commission-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 112px;
  margin-bottom: 16px;
  padding: 0 38px;
  color: #fff;
  border-radius: 6px;
  background: linear-gradient(105deg, #3e7cf5 0%, #6368f5 55%, #9360f2 100%);
}

.commission-label {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 600;
}

.commission-help { opacity: 0.65; font-size: 13px; }
.commission-bar > strong { font-size: 28px; }

.settlement-tip {
  display: flex;
  align-items: center;
  min-height: 47px;
  margin-bottom: 20px;
  padding: 0 18px;
  color: #2461b3;
  font-size: 13px;
  font-weight: 600;
  background: #edf4ff;
  border: 1px solid #aecdff;
  border-radius: 7px;
}

.settlement-tip i { margin-right: 12px; }

.detail-panel {
  overflow: hidden;
  background: #fff;
  border: 1px solid #edf0f5;
  border-radius: 4px;
  box-shadow: 0 5px 14px rgba(31, 45, 61, 0.07);
}

.detail-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 50px;
  padding: 0 15px;
  border-bottom: 1px solid #e7ebf0;
}

.detail-header strong { font-size: 15px; }
.detail-header span { color: #8b9bb0; font-size: 12px; }
.loading-stage i { margin-right: 4px; }
.table-wrap { padding: 15px 20px 20px; overflow-x: auto; }
.table-wrap ::v-deep .el-table { min-width: 1160px; color: #5f6a7c; }
.table-wrap ::v-deep th.el-table__cell { background: #f7f9fb; color: #657083; font-weight: 600; }
.table-wrap ::v-deep .el-table__cell { height: 44px; padding: 0; }
.subid-link { color: #1890ff; }
.metric-value { color: #67c23a; }

@media (max-width: 1400px) {
  .dashboard-form { flex-wrap: wrap; gap: 12px 0; }
  .dashboard-form ::v-deep .el-form-item { margin-right: 18px; }
  .dashboard-form ::v-deep .el-date-editor { width: 360px; }
}

@media (max-width: 992px) {
  .metric-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .dashboard-form { align-items: stretch; }
}

@media (max-width: 620px) {
  .dashboard-page { padding: 12px; }
  .metric-grid { grid-template-columns: 1fr; }
  .metric-card { min-height: 100px; padding: 22px; }
  .dashboard-form ::v-deep .el-form-item { width: 100%; margin-right: 0; }
  .dashboard-form ::v-deep .el-form-item__content { flex: 1; }
  .dashboard-form ::v-deep .el-date-editor,
  .dashboard-form ::v-deep .el-select,
  .dashboard-form ::v-deep .el-input { width: 100%; }
  .commission-bar { min-height: 92px; padding: 0 20px; }
  .commission-bar > strong { font-size: 23px; }
}
</style>
