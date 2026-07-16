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

          <el-table :data="mySubIds" border>
            <el-table-column prop="subId" label="SUBID" min-width="120" />
            <el-table-column label="推广链接" min-width="420">
              <template slot-scope="scope">
                <div class="link-cell">
                  <span class="link-text">{{ scope.row.promoLink }}</span>
                  <el-button type="text" @click="copyText(scope.row.promoLink)">复制</el-button>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="owner" label="拥有者" min-width="120" />
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

          <el-table :data="downlineSubIds" border empty-text="暂无数据">
            <el-table-column prop="subId" label="SUBID" min-width="120" />
            <el-table-column label="推广链接" min-width="420">
              <template slot-scope="scope">
                <div class="link-cell">
                  <span class="link-text">{{ scope.row.promoLink }}</span>
                  <el-button type="text" @click="copyText(scope.row.promoLink)">复制</el-button>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="source" label="来源" min-width="120" />
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
const baseLink = 'https://getstartedtiktok.partnerlinks.io/k0p71lz923ri'

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
      open: false,
      form: createForm(),
      mySubIds: [
        {
          subId: 'MY',
          promoLink: `${baseLink}?sid=MY`,
          owner: 'MY',
          bindTime: '2026-07-13 10:49:27'
        },
        {
          subId: '12344',
          promoLink: `${baseLink}?sid=12344`,
          owner: 'MY',
          bindTime: '2026-07-13 10:56:54'
        }
      ],
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
  methods: {
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
      this.$refs.form.validate(valid => {
        if (!valid) {
          return
        }
        const exists = this.mySubIds.some(item => item.subId.toLowerCase() === this.form.subId.toLowerCase())
        if (exists) {
          this.$message.error('该 SubId 已存在')
          return
        }
        this.mySubIds.push({
          subId: this.form.subId,
          promoLink: `${baseLink}?sid=${this.form.subId}`,
          owner: 'MY',
          bindTime: this.parseCurrentTime()
        })
        this.open = false
        this.$message.success('SubId 申请成功（Mock）')
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
    },
    parseCurrentTime() {
      const now = new Date()
      const pad = value => `${value}`.padStart(2, '0')
      return `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())} ${pad(now.getHours())}:${pad(now.getMinutes())}:${pad(now.getSeconds())}`
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
