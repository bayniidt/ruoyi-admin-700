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
            <el-table-column prop="companyName" label="公司名称" min-width="180" />
            <el-table-column prop="partnerCustomerKey" label="PartnerStack客户Key" min-width="180" />
            <el-table-column label="推广链接" min-width="420">
              <template slot-scope="scope">
                <div class="link-cell">
                  <span class="link-text">{{ scope.row.promoLink }}</span>
                  <el-button type="text" @click="copyText(scope.row.promoLink)">复制</el-button>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="owner" label="客户编号" min-width="160" />
            <el-table-column prop="sharedId" label="共享ID" min-width="140" />
            <el-table-column prop="countryName" label="国家/地区" min-width="120" />
            <el-table-column prop="paidLabel" label="付费状态" min-width="100" align="center">
              <template slot-scope="scope">
                <el-tag :type="scope.row.paidType" size="small">{{ scope.row.paidLabel }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="archiveLabel" label="归档状态" min-width="100" align="center">
              <template slot-scope="scope">
                <el-tag :type="scope.row.archiveType" size="small">{{ scope.row.archiveLabel }}</el-tag>
              </template>
            </el-table-column>
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
                  <span class="link-text">{{ scope.row.promoLink }}</span>
                  <el-button type="text" @click="copyText(scope.row.promoLink)">复制</el-button>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="source" label="公司名称" min-width="160" />
            <el-table-column prop="partnerCustomerKey" label="PartnerStack客户Key" min-width="180" />
            <el-table-column prop="countryName" label="国家/地区" min-width="120" />
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
import { getPartnerStackCustomers } from '@/api/partnerstack'

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
        const { data } = await getPartnerStackCustomers({ limit: 100 })
        const list = this.extractItems(data)
        this.mySubIds = list.map((item, index) => this.toSubIdRow(item, index))
        this.downlineSubIds = list.slice(0, 20).map((item, index) => ({
          ...this.toSubIdRow(item, index),
          source: item.name || item.customer_name || 'PartnerStack Customer'
        }))
      } finally {
        this.loading = false
      }
    },
    toSubIdRow(item, index) {
      const customerKey = item.key || item.customer_key || `cus_${index + 1}`
      const subId = customerKey.replace(/^cus_/i, '').slice(0, 12) || `SID${index + 1}`
      return {
        subId,
        promoLink: `${baseLink}?sid=${subId}`,
        companyName: item.company?.name || '-',
        partnerCustomerKey: item.key || '-',
        owner: item.customer_key || item.shared_id || item.key || '未知客户',
        sharedId: item.shared_id || '-',
        countryName: this.toCountryName(item.country_iso),
        paidLabel: item.has_paid ? '已付费' : '未付费',
        paidType: item.has_paid ? 'success' : 'info',
        archiveLabel: item.archived ? '已归档' : '生效中',
        archiveType: item.archived ? 'info' : 'success',
        bindTime: this.formatTime(item.created_at || item.createdAt)
      }
    },
    extractItems(data) {
      if (!data) {
        return []
      }
      const content = data.data || data
      return Array.isArray(content) ? content : (content.items || content.results || [])
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
      this.$refs.form.validate(valid => {
        if (!valid) {
          return
        }
        const exists = this.mySubIds.some(item => item.subId.toLowerCase() === this.form.subId.toLowerCase())
        if (exists) {
          this.$message.error('该 SubId 已存在')
          return
        }
        this.mySubIds.unshift({
          subId: this.form.subId,
          promoLink: `${baseLink}?sid=${this.form.subId}`,
          partnerCustomerKey: '-',
          owner: 'Manual',
          sharedId: '-',
          countryName: '-',
          paidLabel: '未付费',
          paidType: 'info',
          archiveLabel: '生效中',
          archiveType: 'success',
          bindTime: this.parseCurrentTime()
        })
        this.open = false
        this.$message.success('SubId 已加入当前列表')
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
    },
    formatTime(value) {
      if (!value) {
        return '-'
      }
      const date = new Date(Number(value))
      if (Number.isNaN(date.getTime())) {
        return '-'
      }
      const pad = num => `${num}`.padStart(2, '0')
      return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
    },
    toCountryName(code) {
      const map = {
        US: '美国',
        PH: '菲律宾',
        TH: '泰国',
        HK: '中国香港',
        GB: '英国',
        CA: '加拿大'
      }
      return map[code] || code || '-'
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
