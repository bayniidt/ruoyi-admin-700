<template>
  <div class="dashboard-page">
    <div class="dashboard-hero">
      <div>
        <p class="dashboard-eyebrow">Overview Dashboard</p>
        <h1>渠道数据总览</h1>
        <p class="dashboard-subtitle">
          用 mock 数据先把首页结构跑通，后续统一替换为真实接口。
        </p>
      </div>
      <div class="hero-amount">
        <span>预估分销佣金</span>
        <strong>${{ summary.estimatedCommission }}</strong>
      </div>
    </div>

    <el-card shadow="never" class="filter-panel">
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
          <el-select v-model="filters.subId" placeholder="请选择" clearable>
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

    <el-row :gutter="16" class="metric-grid">
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
          <el-table :data="tableData" stripe>
            <el-table-column prop="subId" label="SUBID" min-width="120" />
            <el-table-column prop="clicks" label="总点击" align="right" />
            <el-table-column prop="uniqueClicks" label="不重复点击" align="right" />
            <el-table-column prop="registrations" label="注册" align="right" />
            <el-table-column prop="paidRegistrations" label="付费注册" align="right" />
            <el-table-column prop="actions" label="动作数" align="right" />
            <el-table-column prop="validActions" label="有效动作数" align="right" />
            <el-table-column prop="spend" label="有效消耗" align="right">
              <template slot-scope="scope">${{ scope.row.spend }}</template>
            </el-table-column>
            <el-table-column prop="commission" label="预估分销佣金" align="right">
              <template slot-scope="scope">${{ scope.row.commission }}</template>
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
            <strong>{{ summary.uniqueClicks }} 个有效点击</strong>
            <strong>{{ summary.registrations }} 个注册</strong>
          </div>
        </el-card>

        <el-card shadow="never" class="mini-trend-card">
          <div slot="header" class="card-header">
            <span>今日摘要</span>
          </div>
          <div class="trend-row">
            <span>点击转化率</span>
            <strong>18.7%</strong>
          </div>
          <div class="trend-row">
            <span>注册转化率</span>
            <strong>0.0%</strong>
          </div>
          <div class="trend-row">
            <span>付费注册占比</span>
            <strong>0.0%</strong>
          </div>
          <div class="trend-row">
            <span>平均单次消耗</span>
            <strong>$0.00</strong>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script>
export default {
  name: 'Index',
  data() {
    const defaultRange = ['2026-07-01 00:00:00', '2026-07-16 23:59:59']
    return {
      filters: {
        dateRange: defaultRange,
        subId: '',
        transactionId: ''
      },
      subIdOptions: ['MY', '12344', 'demo-subid'],
      summary: {
        rawClicks: 6,
        uniqueClicks: 6,
        registrations: 0,
        paidRegistrations: 0,
        actions: 0,
        validActions: 0,
        spend: '0.00',
        estimatedCommission: '0.00'
      },
      tableData: [
        {
          subId: 'MY',
          clicks: 6,
          uniqueClicks: 6,
          registrations: 0,
          paidRegistrations: 0,
          actions: 0,
          validActions: 0,
          spend: '0.00',
          commission: '0.00'
        },
        {
          subId: '12344',
          clicks: 0,
          uniqueClicks: 0,
          registrations: 0,
          paidRegistrations: 0,
          actions: 0,
          validActions: 0,
          spend: '0.00',
          commission: '0.00'
        }
      ]
    }
  },
  computed: {
    metricCards() {
      return [
        {
          key: 'rawClicks',
          label: '原始点击',
          value: this.summary.rawClicks,
          icon: 'el-icon-view',
          color: 'linear-gradient(135deg, #6f7bf7, #8e66ff)'
        },
        {
          key: 'uniqueClicks',
          label: '不重复点击',
          value: this.summary.uniqueClicks,
          icon: 'el-icon-thumb',
          color: 'linear-gradient(135deg, #5f8bff, #6ca8ff)'
        },
        {
          key: 'registrations',
          label: '注册数',
          value: this.summary.registrations,
          icon: 'el-icon-user',
          color: 'linear-gradient(135deg, #23b7a4, #49d3a8)'
        },
        {
          key: 'paidRegistrations',
          label: '付费注册数',
          value: this.summary.paidRegistrations,
          icon: 'el-icon-s-custom',
          color: 'linear-gradient(135deg, #f58a66, #f7b267)'
        },
        {
          key: 'actions',
          label: '动作数',
          value: this.summary.actions,
          icon: 'el-icon-data-analysis',
          color: 'linear-gradient(135deg, #f0b429, #f7d154)'
        },
        {
          key: 'validActions',
          label: '有效动作数',
          value: this.summary.validActions,
          icon: 'el-icon-histogram',
          color: 'linear-gradient(135deg, #4d7cff, #58c0ff)'
        },
        {
          key: 'spend',
          label: '有效消耗',
          value: `$${this.summary.spend}`,
          icon: 'el-icon-wallet',
          color: 'linear-gradient(135deg, #8b5cf6, #c084fc)'
        }
      ]
    }
  },
  methods: {
    handleSearch() {
      this.$message.success('已按 mock 条件完成查询')
    },
    handleReset() {
      this.filters = {
        dateRange: ['2026-07-01 00:00:00', '2026-07-16 23:59:59'],
        subId: '',
        transactionId: ''
      }
      this.$message.success('筛选条件已重置')
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
