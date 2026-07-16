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
      <el-table :data="filteredList" border>
        <el-table-column prop="accountId" label="广告户ID" min-width="180" />
        <el-table-column prop="subId" label="SubId" min-width="120" />
        <el-table-column prop="countryCode" label="国家代码" min-width="110" align="center" />
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
const defaultRange = ['2026-06-16 15:19:58', '2026-07-16 15:19:58']

const mockAccounts = [
  {
    accountId: 'act_9052101871',
    subId: 'MY',
    countryCode: 'US',
    status: 'active',
    statusLabel: '正常',
    statusType: 'success',
    spend: '245.80',
    updateTime: '2026-07-15 22:18:33',
    createTime: '2026-07-02 13:10:26'
  },
  {
    accountId: 'act_9052101872',
    subId: '12344',
    countryCode: 'GB',
    status: 'pending',
    statusLabel: '审核中',
    statusType: 'warning',
    spend: '0.00',
    updateTime: '2026-07-14 09:46:51',
    createTime: '2026-07-10 18:02:14'
  },
  {
    accountId: 'act_9052101873',
    subId: 'MY_2026',
    countryCode: 'CA',
    status: 'disabled',
    statusLabel: '已停用',
    statusType: 'info',
    spend: '18.50',
    updateTime: '2026-07-13 11:27:08',
    createTime: '2026-06-29 08:21:40'
  }
]

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
      subIdOptions: ['MY', '12344', 'MY_2026'],
      statusOptions: [
        { label: '正常', value: 'active' },
        { label: '审核中', value: 'pending' },
        { label: '已停用', value: 'disabled' }
      ],
      accountList: mockAccounts
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
  methods: {
    handleSearch() {
      this.$message.success('已按 mock 条件完成搜索')
    },
    handleReset() {
      this.filters = {
        dateRange: defaultRange,
        accountId: '',
        subId: '',
        status: '',
        minSpend: 0
      }
      this.$message.success('筛选条件已重置')
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
