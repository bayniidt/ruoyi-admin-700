<template>
  <div class="downline-page">
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
        <el-form-item>
          <el-button type="primary" icon="el-icon-search" @click="handleSearch">查询</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <div class="stats-grid">
      <el-card v-for="item in statCards" :key="item.key" shadow="hover" class="stat-card">
        <div class="stat-icon" :style="{ background: item.color }">
          <i :class="item.icon"></i>
        </div>
        <div class="stat-content">
          <span>{{ item.label }}</span>
          <strong>{{ item.value }}</strong>
        </div>
      </el-card>
    </div>

    <el-card shadow="never" class="table-card">
      <el-table :data="tableData" border>
        <el-table-column prop="agentName" label="下级名称" min-width="140" />
        <el-table-column prop="commissionRate" label="佣金比例" min-width="110" align="center">
          <template slot-scope="scope">
            <span>{{ scope.row.commissionRate }}%</span>
          </template>
        </el-table-column>
        <el-table-column prop="subId" label="SUBID" min-width="120" />
        <el-table-column prop="clicks" label="总点击" min-width="110" align="right" />
        <el-table-column prop="uniqueClicks" label="不重复点击" min-width="120" align="right" />
        <el-table-column prop="registrations" label="注册" min-width="90" align="right" />
        <el-table-column prop="paidRegistrations" label="付费注册" min-width="110" align="right" />
        <el-table-column prop="actions" label="动作" min-width="90" align="right" />
        <el-table-column prop="validActions" label="有效动作" min-width="110" align="right" />
        <el-table-column prop="spend" label="有效消耗" min-width="110" align="right">
          <template slot-scope="scope">
            <span>${{ scope.row.spend }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="downlineCommission" label="下级佣金" min-width="110" align="right">
          <template slot-scope="scope">
            <span>${{ scope.row.downlineCommission }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="estimatedRevenue" label="预估收益" min-width="110" align="right">
          <template slot-scope="scope">
            <span>${{ scope.row.estimatedRevenue }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="estimatedProfit" label="预估利润" min-width="110" align="right">
          <template slot-scope="scope">
            <span>${{ scope.row.estimatedProfit }}</span>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script>
const defaultRange = ['2026-06-16 15:14:07', '2026-07-16 15:14:07']

const tableData = [
  {
    agentName: 'PK Team',
    commissionRate: 20,
    subId: 'PK-001',
    clicks: 0,
    uniqueClicks: 0,
    registrations: 0,
    paidRegistrations: 0,
    actions: 0,
    validActions: 0,
    spend: '0.00',
    downlineCommission: '0.00',
    estimatedRevenue: '0.00',
    estimatedProfit: '0.00'
  },
  {
    agentName: 'MY Growth',
    commissionRate: 18,
    subId: 'MY-2026',
    clicks: 12,
    uniqueClicks: 9,
    registrations: 2,
    paidRegistrations: 1,
    actions: 3,
    validActions: 2,
    spend: '124.50',
    downlineCommission: '26.80',
    estimatedRevenue: '165.00',
    estimatedProfit: '40.50'
  }
]

export default {
  name: 'DownlineData',
  data() {
    return {
      filters: {
        dateRange: defaultRange
      },
      tableData
    }
  },
  computed: {
    summary() {
      return this.tableData.reduce((acc, item) => {
        acc.downlineCount += 1
        acc.clicks += item.clicks
        acc.uniqueClicks += item.uniqueClicks
        acc.registrations += item.registrations
        acc.paidRegistrations += item.paidRegistrations
        acc.actions += item.actions
        acc.validActions += item.validActions
        acc.spend += Number(item.spend)
        acc.downlineCommission += Number(item.downlineCommission)
        acc.estimatedRevenue += Number(item.estimatedRevenue)
        acc.estimatedProfit += Number(item.estimatedProfit)
        return acc
      }, {
        downlineCount: 0,
        clicks: 0,
        uniqueClicks: 0,
        registrations: 0,
        paidRegistrations: 0,
        actions: 0,
        validActions: 0,
        spend: 0,
        downlineCommission: 0,
        estimatedRevenue: 0,
        estimatedProfit: 0
      })
    },
    statCards() {
      return [
        {
          key: 'downlineCount',
          label: '下级数量',
          value: this.summary.downlineCount,
          icon: 'el-icon-user',
          color: 'linear-gradient(135deg, #8b5cf6, #a855f7)'
        },
        {
          key: 'clicks',
          label: '点击数',
          value: this.summary.clicks,
          icon: 'el-icon-thumb',
          color: 'linear-gradient(135deg, #4f7cff, #3b82f6)'
        },
        {
          key: 'uniqueClicks',
          label: '唯一点击数',
          value: this.summary.uniqueClicks,
          icon: 'el-icon-finished',
          color: 'linear-gradient(135deg, #4f7cff, #60a5fa)'
        },
        {
          key: 'actions',
          label: '动作数',
          value: this.summary.actions,
          icon: 'el-icon-data-analysis',
          color: 'linear-gradient(135deg, #4f7cff, #5b8cff)'
        },
        {
          key: 'validActions',
          label: '有效动作数',
          value: this.summary.validActions,
          icon: 'el-icon-histogram',
          color: 'linear-gradient(135deg, #4f7cff, #7aa2ff)'
        },
        {
          key: 'registrations',
          label: '注册数',
          value: this.summary.registrations,
          icon: 'el-icon-data-line',
          color: 'linear-gradient(135deg, #4f7cff, #68a1ff)'
        },
        {
          key: 'paidRegistrations',
          label: '付费注册数',
          value: this.summary.paidRegistrations,
          icon: 'el-icon-s-check',
          color: 'linear-gradient(135deg, #4f7cff, #79a7ff)'
        },
        {
          key: 'spend',
          label: '有效消耗',
          value: `$${this.summary.spend.toFixed(2)}`,
          icon: 'el-icon-coin',
          color: 'linear-gradient(135deg, #8b5cf6, #9b5de5)',
          accent: true
        },
        {
          key: 'downlineCommission',
          label: '下级佣金合计',
          value: `$${this.summary.downlineCommission.toFixed(2)}`,
          icon: 'el-icon-office-building',
          color: 'linear-gradient(135deg, #f59e0b, #fb923c)',
          accent: true
        },
        {
          key: 'estimatedRevenue',
          label: '预估收益合计',
          value: `$${this.summary.estimatedRevenue.toFixed(2)}`,
          icon: 'el-icon-camera',
          color: 'linear-gradient(135deg, #4f7cff, #3b82f6)',
          accent: true
        },
        {
          key: 'estimatedProfit',
          label: '预估利润合计',
          value: `$${this.summary.estimatedProfit.toFixed(2)}`,
          icon: 'el-icon-shopping-bag-2',
          color: 'linear-gradient(135deg, #52c41a, #84cc16)',
          accent: true
        }
      ]
    }
  },
  methods: {
    handleSearch() {
      this.$message.success('已按 mock 条件刷新下级数据')
    }
  }
}
</script>

<style lang="scss" scoped>
.downline-page {
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

.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 18px;
  margin-bottom: 18px;
}

.stat-card {
  border: 0;
  border-radius: 18px;

  ::v-deep .el-card__body {
    display: flex;
    align-items: center;
    gap: 16px;
    min-height: 88px;
  }
}

.stat-icon {
  width: 46px;
  height: 46px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 20px;
  flex-shrink: 0;
}

.stat-content {
  display: flex;
  flex-direction: column;
  gap: 6px;

  span {
    color: #707b96;
    font-size: 14px;
  }

  strong {
    color: #1f2433;
    font-size: 18px;
    line-height: 1;
  }
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
