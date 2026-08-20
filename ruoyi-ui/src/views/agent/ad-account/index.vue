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
            :default-time="['00:00:00', '23:59:59']"
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
            <el-button type="text" class="account-link" @click="openActionDialog(scope.row)">{{ scope.row.accountId }}</el-button>
          </template>
        </el-table-column>
        <el-table-column prop="subId" label="SubId" min-width="150" align="center">
          <template slot-scope="scope">
            <el-button
              type="text"
              class="subid-link"
              :disabled="!scope.row.subId || scope.row.subId === '-'"
              @click="openActionDialog(scope.row)"
            >{{ scope.row.subId || '-' }}</el-button>
          </template>
        </el-table-column>
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
      :title="`SubId: ${actionDialog.subId} 的行动列表`"
      :visible.sync="actionDialog.visible"
      width="900px"
      append-to-body
      class="action-dialog"
      @closed="resetActionDialog"
    >
      <div class="action-toolbar">
        <div class="action-summary">
          共 <strong>{{ actionDialog.total }}</strong> 个行动，
          总有效消耗: <strong>${{ formatMoney(actionDialog.totalAmount) }}</strong>
        </div>
        <div class="action-tools">
          <el-input
            v-model.trim="actionDialog.customerKey"
            placeholder="搜索广告账户ID"
            clearable
            prefix-icon="el-icon-search"
            @keyup.enter.native="searchActionRecords"
            @clear="searchActionRecords"
          />
          <el-button
            type="primary"
            icon="el-icon-download"
            :loading="actionDialog.exporting"
            @click="exportActionRecords"
          >导出</el-button>
        </div>
      </div>

      <el-table
        v-loading="actionDialog.loading"
        :data="actionDialog.rows"
        border
        empty-text="暂无行动数据"
      >
        <el-table-column prop="transactionKey" label="交易 ID" min-width="310" />
        <el-table-column prop="amountUsd" label="有效消耗" width="150" align="right">
          <template slot-scope="scope">${{ formatMoney(scope.row.amountUsd) }}</template>
        </el-table-column>
        <el-table-column prop="createdAt" label="交易时间" width="180" />
        <el-table-column prop="status" label="状态" width="110" align="center">
          <template slot-scope="scope">
            <el-tag :type="actionStatusType(actionStatusLabel(scope.row.status))" effect="plain">
              {{ actionStatusLabel(scope.row.status) }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-if="actionDialog.total > actionDialog.pageSize"
        class="action-pagination"
        background
        layout="prev, pager, next, total"
        :current-page="actionDialog.pageNum"
        :page-size="actionDialog.pageSize"
        :total="actionDialog.total"
        @current-change="changeActionPage"
      />

      <span slot="footer">
        <el-button @click="actionDialog.visible = false">关闭</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import {
  getPartnerStackActionRecords,
  getPartnerStackAdAccounts
} from '@/api/partnerstack'
import { buildDateRangeParams } from './dateRange'

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
      actionDialog: {
        visible: false,
        loading: false,
        exporting: false,
        requestId: 0,
        subId: '',
        customerKey: '',
        dateRange: [],
        pageNum: 1,
        pageSize: 10,
        total: 0,
        totalAmount: 0,
        rows: []
      }
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
      return buildDateRangeParams(this.filters.dateRange)
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
    async openActionDialog(row) {
      if (!row.subId || row.subId === '-') return
      this.actionDialog.visible = true
      this.actionDialog.subId = row.subId
      this.actionDialog.customerKey = ''
      this.actionDialog.dateRange = [...(this.filters.dateRange || [])]
      this.actionDialog.pageNum = 1
      await this.fetchActionRecords()
    },
    actionRecordParams(includePagination = true) {
      const params = {
        subId: this.actionDialog.subId,
        ...buildDateRangeParams(this.actionDialog.dateRange),
        customerKey: this.actionDialog.customerKey || undefined
      }
      if (includePagination) {
        params.pageNum = this.actionDialog.pageNum
        params.pageSize = this.actionDialog.pageSize
      }
      return params
    },
    async fetchActionRecords() {
      const requestId = ++this.actionDialog.requestId
      this.actionDialog.loading = true
      try {
        const response = await getPartnerStackActionRecords(this.actionRecordParams())
        if (requestId !== this.actionDialog.requestId) return
        const data = response.data || {}
        this.actionDialog.rows = data.rows || []
        this.actionDialog.total = Number(data.total || 0)
        this.actionDialog.totalAmount = Number(data.totalAmount || 0)
      } finally {
        if (requestId === this.actionDialog.requestId) {
          this.actionDialog.loading = false
        }
      }
    },
    searchActionRecords() {
      this.actionDialog.pageNum = 1
      this.fetchActionRecords()
    },
    changeActionPage(pageNum) {
      this.actionDialog.pageNum = pageNum
      this.fetchActionRecords()
    },
    async exportActionRecords() {
      this.actionDialog.exporting = true
      const safeSubId = this.actionDialog.subId.replace(/[^a-zA-Z0-9_-]/g, '_')
      try {
        await this.download(
          '/partnerstack/action-records/export',
          this.actionRecordParams(false),
          `transaction_${safeSubId}_${new Date().getTime()}.xlsx`
        )
      } finally {
        this.actionDialog.exporting = false
      }
    },
    resetActionDialog() {
      this.actionDialog.requestId += 1
      Object.assign(this.actionDialog, {
        loading: false,
        exporting: false,
        subId: '',
        customerKey: '',
        dateRange: [],
        pageNum: 1,
        total: 0,
        totalAmount: 0,
        rows: []
      })
    },
    actionStatusType(status) {
      return status === '已通过' ? 'success' : 'warning'
    },
    actionStatusLabel(status) {
      return status === '已通过' ? '已通过' : '待审核'
    },
    formatMoney(value) {
      return Number(value || 0).toFixed(2)
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

.subid-link {
  padding: 0;
  color: #1890ff;
}

.action-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  margin: -8px 0 16px;
  padding: 16px;
  background: #f7f9fc;
  border-radius: 6px;
}

.action-summary {
  flex: 1;
  color: #909399;
}

.action-summary strong {
  color: #303133;
}

.action-tools {
  display: flex;
  gap: 12px;
}

.action-tools ::v-deep .el-input {
  width: 280px;
}

.action-pagination {
  margin-top: 16px;
  text-align: right;
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

</style>
