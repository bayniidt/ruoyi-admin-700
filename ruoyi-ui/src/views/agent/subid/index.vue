<template>
  <div class="subid-page">
    <el-card shadow="never" class="subid-card">
      <el-tabs v-model="activeTab" class="subid-tabs">
        <el-tab-pane label="我的 SubId" name="mine">
          <div class="section-header">
            <div class="section-title">
              <h3>我的 SubId</h3>
              <span>{{ mySubIds.length }} / 100</span>
            </div>
            <el-button type="primary" icon="el-icon-plus" @click="openDialog">申请 SubId</el-button>
          </div>

          <el-table :data="mySubIds" border v-loading="loading">
            <el-table-column prop="subId" label="SUBID" min-width="120" />
            <el-table-column label="推广链接" min-width="420">
              <template slot-scope="scope">
                <div class="link-cell">
                  <span class="link-text">{{ scope.row.promoLink || '-' }}</span>
                  <el-button v-if="scope.row.promoLink" type="text" @click="copyText(scope.row.promoLink)">复制</el-button>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="ownerLabel" label="拥有者" min-width="160" />
            <el-table-column prop="bindTime" label="绑定时间" min-width="180" />
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="下级 SubId" name="downline">
          <div class="section-header section-header--compact">
            <div class="section-title">
              <h3>下级 SubId</h3>
              <span>共有{{ downlineSubIds.length }}个</span>
            </div>
          </div>

          <el-table :data="downlineSubIds" border empty-text="暂无数据" v-loading="loading">
            <el-table-column prop="subId" label="SUBID" min-width="120" />
            <el-table-column label="推广链接" min-width="420">
              <template slot-scope="scope">
                <div class="link-cell">
                  <span class="link-text">{{ scope.row.promoLink || '-' }}</span>
                  <el-button v-if="scope.row.promoLink" type="text" @click="copyText(scope.row.promoLink)">复制</el-button>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="sourceLabel" label="来源" min-width="180" />
            <el-table-column prop="bindTime" label="绑定时间" min-width="180" />
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <el-dialog title="申请 SubId" :visible.sync="open" width="420px" append-to-body @close="handleCancel">
      <el-form ref="form" :model="form" :rules="rules" label-width="84px">
        <el-form-item label="SubId 值" prop="subId">
          <el-input
            v-model="form.subId"
            placeholder="输入自定义值（字母、数字、下划线）"
            maxlength="50"
            show-word-limit
          />
        </el-form-item>
      </el-form>

      <div slot="footer" class="dialog-footer">
        <el-button @click="handleCancel">取 消</el-button>
        <el-button type="primary" @click="submitForm">确 定</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { createSubId, getDownlineSubIdList, getMySubIdList } from '@/api/agent-subid'

function createForm() {
  return {
    subId: ''
  }
}

export default {
  name: 'SubIdManage',
  data() {
    return {
      activeTab: 'mine',
      loading: false,
      open: false,
      form: createForm(),
      mySubIds: [],
      downlineSubIds: [],
      rules: {
        subId: [
          { required: true, message: 'SubId 值不能为空', trigger: 'blur' },
          { pattern: /^[A-Za-z0-9_]+$/, message: '仅支持字母、数字和下划线', trigger: 'blur' },
          { min: 2, max: 50, message: '长度必须介于 2 和 50 之间', trigger: 'blur' }
        ]
      }
    }
  },
  created() {
    this.fetchCustomers()
  },
  methods: {
    async fetchCustomers() {
      this.loading = true
      try {
        const [mineResponse, downlineResponse] = await Promise.all([
          getMySubIdList(),
          getDownlineSubIdList()
        ])
        this.mySubIds = this.extractRows(mineResponse)
        this.downlineSubIds = this.extractRows(downlineResponse)
      } finally {
        this.loading = false
      }
    },
    toSubIdRow(item) {
      return {
        subId: item.subid || item.subId || '-',
        promoLink: item.promoLink || item.promo_link || '',
        ownerLabel: item.createdByName || item.ownerLabel || '-',
        sourceLabel: item.source || item.sourceLabel || '-',
        bindTime: item.boundAt || item.bindTime || '-'
      }
    },
    extractRows(response) {
      if (!response) {
        return []
      }
      const rows = response.rows || (response.data && response.data.rows) || []
      return rows.map(item => this.toSubIdRow(item))
    },
    openDialog() {
      this.form = createForm()
      this.open = true
      this.$nextTick(() => {
        this.$refs.form && this.$refs.form.clearValidate()
      })
    },
    handleCancel() {
      this.open = false
      this.form = createForm()
    },
    submitForm() {
      this.$refs.form.validate(async valid => {
        if (!valid) {
          return
        }
        await createSubId({ subId: this.form.subId })
        this.open = false
        this.$message.success('SubId 已加入当前列表')
        this.fetchCustomers()
      })
    },
    copyText(text) {
      if (navigator.clipboard) {
        navigator.clipboard.writeText(text).then(() => {
          this.$message.success('已复制')
        }).catch(() => {
          this.fallbackCopy(text)
        })
        return
      }
      this.fallbackCopy(text)
    },
    fallbackCopy(text) {
      const textarea = document.createElement('textarea')
      textarea.value = text
      textarea.style.position = 'fixed'
      textarea.style.opacity = '0'
      document.body.appendChild(textarea)
      textarea.focus()
      textarea.select()
      document.execCommand('copy')
      document.body.removeChild(textarea)
      this.$message.success('已复制')
    }
  }
}
</script>

<style lang="scss" scoped>
.subid-page {
  padding: 4px;
  background: #f5f7fb;
}

.subid-card {
  border-radius: 18px;
}

.subid-tabs ::v-deep .el-tabs__header {
  margin-bottom: 18px;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
}

.section-header--compact {
  justify-content: flex-start;
}

.section-title {
  display: flex;
  align-items: baseline;
  gap: 10px;

  h3 {
    margin: 0;
    font-size: 30px;
    font-weight: 700;
    color: #1f2433;
  }

  span {
    color: #8b94ad;
    font-size: 14px;
  }
}

.link-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.link-text {
  color: #5d6785;
  word-break: break-all;
}

.subid-card ::v-deep .el-table__header th {
  background: #f7f9fc;
  color: #5d6785;
  font-weight: 600;
}

@media (max-width: 768px) {
  .section-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .section-title h3 {
    font-size: 22px;
  }

  .link-cell {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
