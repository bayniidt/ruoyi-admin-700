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
        <el-table-column prop="accountId" label="广告户ID" min-width="180">
          <template slot-scope="scope">
            <el-button type="text" class="account-link" @click="openTransactionDialog(scope.row)">{{ scope.row.accountId }}</el-button>
          </template>
        </el-table-column>
        <el-table-column prop="subId" label="SubId" min-width="120" align="center" />
        <el-table-column prop="countryCode" label="国家代码" min-width="120" align="center" />
        <el-table-column prop="statusLabel" label="状态" min-width="110" align="center">
          <template slot-scope="scope">
            <el-tag :type="scope.row.statusType" size="small">{{ scope.row.statusLabel }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="spend" label="有效消耗" min-width="120" align="center">
          <template slot-scope="scope">
            <span>${{ scope.row.spend }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="updateTime" label="更新时间" min-width="180" align="center" />
        <el-table-column prop="createTime" label="创建时间" min-width="180" align="center" />
      </el-table>
    </el-card>

    <el-dialog
      :title="`广告户ID: ${dialog.customerKey || '-'} 的行动列表`"
      :visible.sync="dialog.open"
      width="900px"
      append-to-body
      class="transaction-dialog"
    >
      <div class="dialog-toolbar">
        <div class="dialog-summary">
          <span>共 {{ filteredTransactions.length }} 个行动</span>
          <span>总有效消耗: <strong>${{ totalDialogSpend }}</strong></span>
        </div>
        <div class="dialog-actions">
          <el-input v-model="dialog.keyword" placeholder="搜索广告账户ID" clearable />
          <el-button type="primary" icon="el-icon-download" @click="handleExportTransactions">导出</el-button>
        </div>
      </div>

      <el-table :data="filteredTransactions" border v-loading="dialog.loading">
        <el-table-column prop="transactionId" label="交易 ID" min-width="220" />
        <el-table-column prop="amountSUM" label="有效消耗" min-width="140" align="center">
          <template slot-scope="scope">
            <span>${{ scope.row.amountSUM }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="transactionTime" label="交易时间" min-width="180" align="center" />
        <el-table-column prop="status" label="状态" min-width="120" align="center" />
      </el-table>

      <div slot="footer">
        <el-button @click="dialog.open = false">关闭</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { getPartnerStackAdAccounts, getPartnerStackTransactionDetails } from '@/api/partnerstack'

const defaultRange = ['2026-06-17 15:03:13', '2026-07-17 15:03:13']

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
      accountList: [],
      dialog: {
        open: false,
        loading: false,
        customerKey: '',
        subId: '',
        keyword: '',
        rows: []
      }
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
    },
    filteredTransactions() {
      return this.dialog.rows.filter(item => {
        return !this.dialog.keyword || item.transactionId.toLowerCase().includes(this.dialog.keyword.toLowerCase())
      })
    },
    totalDialogSpend() {
      return this.filteredTransactions.reduce((sum, item) => sum + Number(item.amountSUM || 0), 0).toFixed(2)
    }
  },
  created() {
    this.fetchAccounts()
  },
  methods: {
    async fetchAccounts() {
      this.loading = true
      try {
        const response = await getPartnerStackAdAccounts({
          ...this.buildRangeParams(),
          customerKey: this.filters.accountId || undefined,
          subId: this.filters.subId || undefined,
          minAmountSUM: this.filters.minSpend || 0,
          pageNum: 1,
          pageSize: 10
        })
        const list = this.extractRows(response)
        this.accountList = list.map(item => {
          const status = this.normalizeStatus(item.hasPaid)
          return {
            accountId: item.customerKey || '-',
            subId: item.subId || '-',
            countryCode: item.countryIso || '-',
            status,
            statusLabel: this.toStatusLabel(status),
            statusType: this.toStatusType(status),
            spend: Number(item.amountSUM || 0).toFixed(2),
            updateTime: this.formatTime(item.updatedAt),
            createTime: this.formatTime(item.createdAt)
          }
        })
        this.subIdOptions = [...new Set(this.accountList.map(item => item.subId).filter(item => item && item !== '-'))]
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
    extractRows(response) {
      if (!response) {
        return []
      }
      return response.data && response.data.rows ? response.data.rows : []
    },
    normalizeStatus(hasPaid) {
      return Number(hasPaid) > 0 ? 'paid' : 'registered'
    },
    toStatusLabel(status) {
      return {
        registered: '注册',
        paid: '付费'
      }[status] || '注册'
    },
    toStatusType(status) {
      return {
        registered: 'primary',
        paid: 'success'
      }[status] || 'info'
    },
    formatTime(value) {
      if (!value) {
        return '-'
      }
      const date = new Date(value)
      if (Number.isNaN(date.getTime())) {
        return '-'
      }
      const pad = num => `${num}`.padStart(2, '0')
      return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
    },
    handleSearch() {
      this.fetchAccounts()
    },
    handleReset() {
      this.filters = {
        dateRange: defaultRange,
        accountId: '',
        subId: '',
        status: '',
        minSpend: 0
      }
      this.fetchAccounts()
    },
    async openTransactionDialog(row) {
      this.dialog.open = true
      this.dialog.loading = true
      this.dialog.customerKey = row.accountId
      this.dialog.subId = row.subId
      this.dialog.keyword = ''
      try {
        const response = await getPartnerStackTransactionDetails({
          customerKey: row.accountId,
          subId: row.subId
        })
        const rows = response.data && response.data.rows ? response.data.rows : []
        this.dialog.rows = rows.map(item => ({
          transactionId: item.transactionId || '-',
          amountSUM: Number(item.amountSUM || 0).toFixed(2),
          transactionTime: item.transactionTime || '-',
          status: item.status || '-'
        }))
      } finally {
        this.dialog.loading = false
      }
    },
    handleExportTransactions() {
      const header = ['交易 ID', '有效消耗', '交易时间', '状态']
      const lines = this.filteredTransactions.map(item => [item.transactionId, item.amountSUM, item.transactionTime, item.status])
      const csvContent = [header, ...lines].map(row => row.join(',')).join('\n')
      const blob = new Blob(['\uFEFF' + csvContent], { type: 'text/csv;charset=utf-8;' })
      const link = document.createElement('a')
      link.href = URL.createObjectURL(blob)
      link.download = `${this.dialog.customerKey || 'transactions'}.csv`
      link.click()
      URL.revokeObjectURL(link.href)
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

.account-link {
  padding: 0;
  font-size: 16px;
  font-weight: 500;
}

.dialog-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 16px;
  margin-bottom: 18px;
  background: #f7f9fc;
  border-radius: 12px;
}

.dialog-summary {
  display: flex;
  gap: 16px;
  color: #5d6785;
  font-size: 18px;
}

.dialog-summary strong {
  color: #303133;
}

.dialog-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.dialog-actions .el-input {
  width: 280px;
}

@media (max-width: 768px) {
  .filter-form {
    display: block;
  }

  .filter-form ::v-deep .el-form-item {
    margin-right: 0;
  }

  .dialog-toolbar,
  .dialog-summary,
  .dialog-actions {
    display: block;
  }
}
</style>
