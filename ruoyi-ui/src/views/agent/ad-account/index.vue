<template>
  <div class="ad-account-page">
    <el-card shadow="never" class="filter-card">
      <el-form :inline="true" :model="filters" class="filter-form">
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
        <el-form-item label="广告户ID">
          <el-input v-model="filters.accountId" placeholder="请输入广告账户ID" clearable />
        </el-form-item>
        <el-form-item label="SubId">
          <el-select v-model="filters.subId" placeholder="全部" clearable>
            <el-option v-for="item in subIdOptions" :key="item" :label="item" :value="item" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="filters.status" placeholder="全部" clearable>
            <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="最小消耗">
          <el-input-number v-model="filters.minSpend" controls-position="right" :min="0" :precision="2" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="el-icon-search" @click="handleSearch">搜索</el-button>
          <el-button icon="el-icon-refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" class="table-card">
      <el-table :data="filteredList" border v-loading="loading">
        <el-table-column prop="accountId" label="广告户ID" min-width="180" />
        <el-table-column prop="transactionAmountUsd" label="交易金额(USD)" min-width="130" align="right">
          <template slot-scope="scope">
            <span>${{ scope.row.transactionAmountUsd }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="subId" label="SubId" min-width="120" />
        <el-table-column prop="companyName" label="公司名称" min-width="180" />
        <el-table-column prop="customerKey" label="客户编号" min-width="180" />
        <el-table-column prop="partnershipKey" label="合作关系Key" min-width="180" />
        <el-table-column prop="productKey" label="产品Key" min-width="140" />
        <el-table-column prop="categoryKey" label="分类Key" min-width="140" />
        <el-table-column prop="currency" label="币种" min-width="90" align="center" />
        <el-table-column prop="statusLabel" label="状态" min-width="110" align="center">
          <template slot-scope="scope">
            <el-tag :type="scope.row.statusType" size="small">{{ scope.row.statusLabel }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="spend" label="有效消耗" min-width="120" align="right">
          <template slot-scope="scope">
            <span>${{ scope.row.spend }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="updateTime" label="更新时间" min-width="180" align="center" />
        <el-table-column prop="createTime" label="创建时间" min-width="180" align="center" />
      </el-table>
    </el-card>
  </div>
</template>

<script>
import { getPartnerStackTransactions } from '@/api/partnerstack'

const defaultRange = ['2026-06-16 15:19:58', '2026-07-16 15:19:58']

export default {
  name: 'AdAccountManage',
  data() {
    return {
      filters: {
        dateRange: defaultRange,
        accountId: '',
        subId: '',
        status: '',
        minSpend: 0
      },
      loading: false,
      subIdOptions: [],
      statusOptions: [
        { label: '正常', value: 'active' },
        { label: '审核中', value: 'pending' },
        { label: '已停用', value: 'disabled' }
      ],
      accountList: []
    }
  },
  computed: {
    filteredList() {
      return this.accountList.filter(item => {
        const matchId = !this.filters.accountId || item.accountId.toLowerCase().includes(this.filters.accountId.toLowerCase())
        const matchSubId = !this.filters.subId || item.subId === this.filters.subId
        const matchStatus = !this.filters.status || item.status === this.filters.status
        const matchSpend = Number(item.spend) >= Number(this.filters.minSpend || 0)
        return matchId && matchSubId && matchStatus && matchSpend
      })
    }
  },
  created() {
    this.fetchTransactions()
  },
  methods: {
    async fetchTransactions() {
      this.loading = true
      try {
        const { data } = await getPartnerStackTransactions({
          ...this.buildRangeParams(),
          limit: 100
        })
        const list = this.extractItems(data)
        this.accountList = list.map((item, index) => {
          const amount = Number(item.amount || item.net_amount || 0) / 100
          const status = this.normalizeStatus(item.archived)
          return {
            accountId: item.key || item.invoice_key || `tran_${index + 1}`,
            transactionAmountUsd: (Number(item.amount_usd || item.amount || 0) / 100).toFixed(2),
            subId: (item.customer?.sub_ids || [])[0] || '-',
            companyName: item.company?.name || '-',
            customerKey: item.customer?.key || '-',
            partnershipKey: item.partnership_key || '-',
            productKey: item.product_key || '-',
            categoryKey: item.category_key || '-',
            currency: item.currency || 'USD',
            status,
            statusLabel: this.toStatusLabel(status),
            statusType: this.toStatusType(status),
            spend: amount.toFixed(2),
            updateTime: this.formatTime(item.updated_at || item.created_at),
            createTime: this.formatTime(item.created_at)
          }
        })
      this.subIdOptions = [...new Set(this.accountList.map(item => item.subId).filter(Boolean))]
      } finally {
        this.loading = false
      }
    },
    buildRangeParams() {
      const [start, end] = this.filters.dateRange || []
      const params = {}
      if (start) {
        params.minCreated = new Date(start).getTime()
      }
      if (end) {
        params.maxCreated = new Date(end).getTime()
      }
      return params
    },
    extractItems(data) {
      if (!data) {
        return []
      }
      const content = data.data || data
      return Array.isArray(content) ? content : (content.items || content.results || [])
    },
    normalizeStatus(archived) {
      return archived ? 'disabled' : 'active'
    },
    toStatusLabel(status) {
      return {
        active: '正常',
        disabled: '已停用'
      }[status] || '正常'
    },
    toStatusType(status) {
      return {
        active: 'success',
        disabled: 'info'
      }[status] || 'info'
    },
    formatTime(value) {
      if (!value) {
        return '-'
      }
      const date = new Date(Number(value))
      if (Number.isNaN(date.getTime())) {
        return '-'
      }
      const pad = num => `${num}`.padStart(2, '0')
      return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
    },
    handleSearch() {
      this.fetchTransactions()
    },
    handleReset() {
      this.filters = {
        dateRange: defaultRange,
        accountId: '',
        subId: '',
        status: '',
        minSpend: 0
      }
      this.fetchTransactions()
    }
  }
}
</script>

<style lang="scss" scoped>
.ad-account-page {
  padding: 4px;
  background: #f5f7fb;
}

.filter-card,
.table-card {
  border-radius: 18px;
  margin-bottom: 18px;
}

.filter-form {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
}

.table-card ::v-deep .el-table__header th {
  background: #f7f9fc;
  color: #5d6785;
  font-weight: 600;
}

@media (max-width: 768px) {
  .filter-form {
    display: block;
  }

  .filter-form ::v-deep .el-form-item {
    margin-right: 0;
  }
}
</style>
