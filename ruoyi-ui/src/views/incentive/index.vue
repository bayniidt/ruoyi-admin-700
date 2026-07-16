<template>
  <div class="incentive-page">
    <el-card shadow="never" class="incentive-card">
      <div class="hero">
        <div>
          <div class="hero-title">
            <h1>Affiliate 推荐激励计划</h1>
            <span>2026年度</span>
          </div>
          <p>推荐优质合作伙伴加入平台，获得阶梯奖金激励。</p>
        </div>
      </div>

      <section class="section">
        <div class="section-heading">
          <i></i>
          <h2>奖励阶梯</h2>
        </div>

        <el-table :data="tierList" border class="tier-table">
          <el-table-column label="梯度" min-width="160">
            <template slot-scope="scope">
              <span class="tier-badge" :class="`tier-${scope.row.level.toLowerCase()}`">{{ scope.row.level }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="spendRange" label="月有效消耗（美金）" min-width="260" align="center" />
          <el-table-column prop="reward" label="奖金（人民币）" min-width="220" align="center">
            <template slot-scope="scope">
              <span class="reward-text">¥{{ scope.row.reward }}</span>
            </template>
          </el-table-column>
        </el-table>
      </section>

      <section class="section">
        <div class="section-heading">
          <i></i>
          <h2>推荐激励机制说明</h2>
        </div>

        <div class="rule-list">
          <div v-for="item in ruleList" :key="item.index" class="rule-item">
            <div class="rule-index">{{ item.index }}</div>
            <div class="rule-body">
              <h3>{{ item.title }}</h3>
              <p>{{ item.content }}</p>
              <div v-if="item.example" class="example-box">
                <span>例：</span>
                <span>{{ item.example }}</span>
              </div>
            </div>
          </div>
        </div>
      </section>
    </el-card>
  </div>
</template>

<script>
export default {
  name: 'IncentivePlan',
  data() {
    return {
      tierList: [
        { level: 'T1', spendRange: 'X ≥ 3万', reward: '2,000' },
        { level: 'T2', spendRange: 'X ≥ 10万', reward: '10,000' },
        { level: 'T3', spendRange: 'X ≥ 30万', reward: '35,000' },
        { level: 'T4', spendRange: 'X ≥ 50万', reward: '60,000' }
      ],
      ruleList: [
        {
          index: 1,
          title: '生效条件',
          content: '推荐激励仅适用于推荐人直接推荐（引荐）合作方与我方直接合作，由我方直接对接合作方案并核算佣金。合作方成为我方一级分销商。推荐人自己发展的下级代理不属于推荐场景，不触发推荐激励。'
        },
        {
          index: 2,
          title: '生效周期',
          content: '三个月。推荐人可获得合作方与我方建立合作并产生业绩后前三个月消耗业绩所对应的推荐激励奖金。',
          example: '推荐人 A 引荐合作方 B 于 2026 年 3 月与我方建立合作，合作方 B 于 4 月产生首次消耗业绩，4、5、6 月消耗分别为 3 万、11 万、32 万，则推荐人 A 可获得奖金 2,000 + 10,000 + 35,000 = 47,000 元。7 月起不再计算。'
        },
        {
          index: 3,
          title: '结算周期与计算规则',
          content: '月度结算，结算时间与 Affiliate 月度佣金结算时间一致。一个合作方对应一个激励区间，推荐多家合作方的激励根据每家对应的激励区间金额相加，多家合作方不可合并计算。',
          example: '推荐人 A 引荐了 B、C、D 三家，首月消耗分别为 3 万、11 万、32 万，对应激励分别为 2,000 + 10,000 + 35,000 = 47,000 元。'
        },
        {
          index: 4,
          title: '执行时间',
          content: '该激励计划适用于 2026 年度内首次建立合作的引荐客户，若后续有政策更新，以平台最新公告为准。'
        }
      ]
    }
  }
}
</script>

<style lang="scss" scoped>
.incentive-page {
  padding: 4px;
  background: #f5f7fb;
}

.incentive-card {
  border-radius: 18px;
}

.hero {
  margin-bottom: 34px;
}

.hero-title {
  display: flex;
  align-items: baseline;
  gap: 14px;
  flex-wrap: wrap;

  h1 {
    margin: 0;
    font-size: 28px;
    font-weight: 700;
    color: #222938;
  }

  span {
    color: #67c23a;
    font-size: 20px;
    font-weight: 500;
  }
}

.hero p {
  margin: 12px 0 0;
  color: #8a93ab;
  font-size: 15px;
}

.section {
  margin-bottom: 34px;
}

.section-heading {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 18px;

  i {
    width: 4px;
    height: 24px;
    border-radius: 999px;
    background: #111827;
    display: inline-block;
  }

  h2 {
    margin: 0;
    font-size: 18px;
    color: #1f2433;
  }
}

.tier-table ::v-deep .el-table__header th {
  background: #f7f9fc;
  color: #7c859d;
  font-weight: 600;
}

.tier-badge {
  display: inline-flex;
  min-width: 42px;
  height: 28px;
  border-radius: 8px;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-weight: 700;
}

.tier-t1,
.tier-t2 {
  background: #9aa0a6;
}

.tier-t3 {
  background: #f5b041;
}

.tier-t4 {
  background: #ff7675;
}

.reward-text {
  color: #ff5a5f;
  font-weight: 700;
}

.rule-list {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.rule-item {
  display: flex;
  gap: 14px;
  align-items: flex-start;
}

.rule-index {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: #409eff;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  flex-shrink: 0;
}

.rule-body {
  h3 {
    margin: 0 0 10px;
    font-size: 16px;
    color: #222938;
  }

  p {
    margin: 0;
    color: #5d6785;
    line-height: 1.9;
  }
}

.example-box {
  margin-top: 14px;
  padding: 14px 16px;
  border-radius: 10px;
  background: #f4f6fa;
  color: #6b7280;
  line-height: 1.8;
}

@media (max-width: 768px) {
  .hero-title h1 {
    font-size: 22px;
  }

  .hero-title span {
    font-size: 16px;
  }

  .rule-item {
    gap: 12px;
  }
}
</style>
