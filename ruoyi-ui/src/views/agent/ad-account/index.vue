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
      <el-table :data="accountList" border v-loading="loading">
        <el-table-column prop="accountId" label="广告户ID" min-width="220">
          <template slot-scope="scope">
            <el-button type="text" class="account-link" @click="openTransactionDialog(scope.row)">{{ scope.row.accountId }}</el-button>
          </template>
        </el-table-column>
        <el-table-column prop="subId" label="SubId" min-width="150" align="center" />
        <el-table-column prop="countryIso" label="国家代码" min-width="130" align="center" />
        <el-table-column prop="statusLabel" label="状态" min-width="120" align="center">
          <template slot-scope="scope">
            <el-tag :type="scope.row.status === 'paid' ? 'success' : ''" effect="plain" size="small">
              {{ scope.row.statusLabel }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="amountSUM" label="有效消耗" min-width="150" align="center">
          <template slot-scope="scope">
            <span>${{ scope.row.amountSUM }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="updatedAt" label="更新时间" min-width="180" align="center" />
        <el-table-column prop="createdAt" label="创建时间" min-width="180" align="center" />
      </el-table>
      <el-pagination
        class="table-pagination"
        background
        layout="total, sizes, prev, pager, next, jumper"
        :current-page.sync="pagination.pageNum"
        :page-size.sync="pagination.pageSize"
        :page-sizes="[10, 20, 50, 100]"
        :total="pagination.total"
        @current-change="handlePageChange"
        @size-change="handleSizeChange"
      />
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

const formatDateTime = date => {
  const pad = value => `${value}`.padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
}

const buildDefaultRange = () => {
  const end = new Date()
  const start = new Date(end.getTime() - 30 * 24 * 60 * 60 * 1000)
  return [formatDateTime(start), formatDateTime(end)]
}

export default {
  name: 'AdAccountManage',
  data() {
    return {
      filters: {
        dateRange: buildDefaultRange(),
        accountId: '',
        subId: '',
        status: '',
        minSpend: 0
      },
      loading: false,
      subIdOptions: [],
      statusOptions: [
        { label: '注册', value: 'registered' },
        { label: '付费', value: 'paid' }
      ],
      accountList: [],
      pagination: {
        pageNum: 1,
        pageSize: 10,
        total: 0
      },
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
          status: this.filters.status || undefined,
          minAmountSUM: this.filters.minSpend || 0,
          pageNum: this.pagination.pageNum,
          pageSize: this.pagination.pageSize
        })
        this.pagination.total = Number(response.data && response.data.total ? response.data.total : 0)
        const list = this.extractRows(response)
        this.accountList = list.map(item => {
          const status = this.normalizeStatus(item.hasPaid)
          return {
            customerKey: item.customerKey || '-',
            accountId: item.contact || item.customerKey || '-',
            contact: item.contact || item.customerKey || '-',
            subId: item.subId || '-',
            countryIso: item.countryIso || '-',
            status,
            statusLabel: this.toStatusLabel(status),
            amountSUM: Number(item.amountSUM || 0).toFixed(2),
            updatedAt: this.formatTime(item.updatedAt),
            createdAt: this.formatTime(item.createdAt)
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
      this.pagination.pageNum = 1
      this.fetchAccounts()
    },
    handleReset() {
      this.filters = {
        dateRange: buildDefaultRange(),
        accountId: '',
        subId: '',
        status: '',
        minSpend: 0
      }
      this.pagination.pageNum = 1
      this.fetchAccounts()
    },
    handlePageChange(page) {
      this.pagination.pageNum = page
      this.fetchAccounts()
    },
    handleSizeChange(size) {
      this.pagination.pageSize = size
      this.pagination.pageNum = 1
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

.table-pagination {
  margin-top: 18px;
  text-align: right;
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
