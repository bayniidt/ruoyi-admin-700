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
        <el-table-column prop="contact" label="联系人" min-width="220">
          <template slot-scope="scope">
            <el-button type="text" class="account-link" @click="openTransactionDialog(scope.row)">{{ scope.row.contact }}</el-button>
          </template>
        </el-table-column>
        <el-table-column prop="statusLabel" label="状态" min-width="110" align="center">
          <template slot-scope="scope">
            <span class="status-dot" :class="scope.row.statusClass"></span>
            <span>{{ scope.row.statusLabel }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="sourceType" label="来源" min-width="140" align="center" />
        <el-table-column prop="revenue" label="收入" min-width="140" align="center">
          <template slot-scope="scope">
            <span>${{ scope.row.revenue }} USD</span>
          </template>
        </el-table-column>
        <el-table-column prop="createdDate" label="日期已创建" min-width="140" align="center" />
      </el-table>
    </el-card>

    <el-dialog
      :title="`${dialog.contact || dialog.customerKey || '-'} 的客户活动`"
      :visible.sync="dialog.open"
      width="900px"
      append-to-body
      class="transaction-dialog"
    >
      <el-empty v-if="!dialog.loading && !filteredTransactions.length" description="暂无活动数据" />
      <el-timeline v-else v-loading="dialog.loading" class="activity-timeline">
        <el-timeline-item
          v-for="item in filteredTransactions"
          :key="item.eventId"
          :timestamp="item.eventDate"
          placement="top"
          color="#e8e8e8"
        >
          <div class="activity-row">
            <span class="activity-desc">{{ item.description }}</span>
            <span class="activity-date">{{ item.eventDate }}</span>
          </div>
        </el-timeline-item>
      </el-timeline>

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
        { label: '已注册', value: 'registered' },
        { label: '已付费', value: 'paid' }
      ],
      accountList: [],
      dialog: {
        open: false,
        loading: false,
        customerKey: '',
        contact: '',
        subId: '',
        rows: []
      }
    }
  },
  computed: {
    filteredList() {
      return this.accountList.filter(item => {
        const matchId = !this.filters.accountId || item.contact.toLowerCase().includes(this.filters.accountId.toLowerCase())
        const matchSubId = !this.filters.subId || item.subId === this.filters.subId
        const matchStatus = !this.filters.status || item.status === this.filters.status
        const matchSpend = Number(item.revenue) >= Number(this.filters.minSpend || 0)
        return matchId && matchSubId && matchStatus && matchSpend
      })
    },
    filteredTransactions() {
      return this.dialog.rows
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
            customerKey: item.customerKey || '-',
            contact: item.contact || item.customerKey || '-',
            subId: item.subId || '-',
            status,
            statusLabel: this.toStatusLabel(status),
            statusClass: this.toStatusClass(status),
            sourceType: item.sourceType || '-',
            revenue: Number(item.totalRevenue || 0).toFixed(2),
            createdDate: item.createdDate || this.formatDate(item.createdAt)
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
        registered: '已注册',
        paid: '已付费'
      }[status] || '已注册'
    },
    toStatusClass(status) {
      return {
        registered: 'signed-up',
        paid: 'paid'
      }[status] || 'signed-up'
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
    formatDate(value) {
      if (!value || value === '-') {
        return '-'
      }
      const date = new Date(value)
      if (Number.isNaN(date.getTime())) {
        return value
      }
      return date.toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' })
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
      this.dialog.customerKey = row.customerKey
      this.dialog.contact = row.contact
      this.dialog.subId = row.subId
      try {
        const response = await getPartnerStackTransactionDetails({
          customerKey: row.customerKey,
          subId: row.subId
        })
        const rows = response.data && response.data.rows ? response.data.rows : []
        this.dialog.rows = rows.map(item => ({
          eventId: item.eventId || '-',
          description: item.description || '-',
          amountSUM: Number(item.amountSUM || 0).toFixed(2),
          eventTime: item.eventTime || '-',
          eventDate: this.formatDate(item.eventTime)
        }))
      } finally {
        this.dialog.loading = false
      }
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

.status-dot {
  display: inline-block;
  width: 8px;
  height: 8px;
  margin-right: 8px;
  border-radius: 50%;
  vertical-align: middle;
}

.status-dot.signed-up {
  background: #5b6cff;
}

.status-dot.paid {
  background: #2bb673;
}

.activity-timeline {
  padding: 12px 8px 0;
}

.activity-row {
  display: flex;
  justify-content: space-between;
  gap: 24px;
  color: #303133;
  font-size: 16px;
  line-height: 24px;
}

.activity-desc {
  flex: 1;
}

.activity-date {
  min-width: 120px;
  color: #606266;
  text-align: right;
}
</style>
