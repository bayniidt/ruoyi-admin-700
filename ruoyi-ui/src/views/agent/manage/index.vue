<template>
  <div class="app-container agent-manage-page">
    <el-card shadow="never" class="page-card">
      <div class="toolbar">
        <el-button type="primary" icon="el-icon-plus" @click="handleAdd">新建代理</el-button>
        <div class="toolbar-search">
          <el-input
            v-model="queryParams.keyword"
            placeholder="用户名"
            clearable
            @keyup.enter.native="handleQuery"
          >
            <el-button slot="append" icon="el-icon-search" @click="handleQuery" />
          </el-input>
        </div>
      </div>

      <el-table :data="agentList" border class="agent-table" v-loading="loading">
        <el-table-column prop="userName" label="用户名" width="210" />
        <el-table-column prop="nickName" label="昵称" width="210">
          <template slot-scope="scope">
            <el-input
              v-model="scope.row.nickName"
              size="mini"
              placeholder="点击输入"
              @blur="handleInlineUpdate(scope.row, 'nickName')"
            />
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" width="210">
          <template slot-scope="scope">
            <el-input
              v-model="scope.row.remark"
              size="mini"
              placeholder="点击输入"
              @blur="handleInlineUpdate(scope.row, 'remark')"
            />
          </template>
        </el-table-column>
        <el-table-column prop="commissionRate" label="分成比例" width="210" align="center">
          <template slot-scope="scope">
            <el-input
              v-model="scope.row.commissionRate"
              type="number"
              placeholder="请输入分成比例"
              min="0"
              :max="maxCommissionRate"
              step="0.01"
              @blur="handleInlineUpdate(scope.row, 'commissionRate')"
            />
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="210" align="center" />
      </el-table>

      <div class="table-footer">
        <el-pagination
          background
          layout="total, sizes, prev, pager, next, jumper"
          :current-page.sync="queryParams.pageNum"
          :page-size.sync="queryParams.pageSize"
          :page-sizes="[20, 30, 50]"
          :total="total"
          @current-change="handleCurrentChange"
          @size-change="handleSizeChange"
        />
      </div>
    </el-card>

    <el-dialog title="新建代理" :visible.sync="open" width="600px" append-to-body @close="handleCancel">
      <el-form ref="form" :model="form" :rules="rules" label-width="96px" class="agent-form">
        <el-form-item label="用户名" prop="userName">
          <el-input v-model="form.userName" placeholder="请输入用户名" maxlength="20" />
        </el-form-item>

        <el-form-item label="用户密码" prop="password">
          <el-input v-model="form.password" type="password" placeholder="请输入用户密码" show-password />
        </el-form-item>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="手机号码" prop="phonenumber">
              <el-input v-model="form.phonenumber" placeholder="请输入手机号码" maxlength="11" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="邮箱" prop="email">
              <el-input v-model="form.email" placeholder="请输入邮箱" maxlength="50" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="用户昵称" prop="nickName">
          <el-input v-model="form.nickName" placeholder="请输入用户昵称" maxlength="20" />
        </el-form-item>

        <el-form-item label="佣金比例" prop="commissionRate">
          <div class="commission-field">
            <el-input-number
              v-model="form.commissionRate"
              controls-position="right"
              :min="0"
              :max="maxCommissionRate"
            />
            <span class="commission-tip">最大佣金比例：{{ maxCommissionRate }}%</span>
          </div>
        </el-form-item>

        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" :rows="3" placeholder="请输入内容" maxlength="200" show-word-limit />
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
import { addAgent, listAgents, updateAgent } from '@/api/agent'

function createDefaultForm() {
  return {
    userName: '',
    password: '',
    phonenumber: '',
    email: '',
    nickName: '',
    commissionRate: 0,
    remark: ''
  }
}

export default {
  name: 'AgentManage',
  data() {
    return {
      open: false,
      loading: false,
      total: 0,
      maxCommissionRate: 100,
      queryParams: {
        keyword: '',
        pageNum: 1,
        pageSize: 20
      },
      agentList: [],
      form: createDefaultForm(),
      rules: {
        userName: [
          { required: true, message: '用户名不能为空', trigger: 'blur' },
          { min: 2, max: 20, message: '用户名长度必须介于 2 和 20 之间', trigger: 'blur' }
        ],
        password: [
          { required: true, message: '用户密码不能为空', trigger: 'blur' }
        ],
        nickName: [
          { required: true, message: '用户昵称不能为空', trigger: 'blur' }
        ],
        email: [
          {
            type: 'email',
            message: '请输入正确的邮箱地址',
            trigger: ['blur', 'change']
          }
        ],
        phonenumber: [
          {
            pattern: /^1[3-9]\d{9}$/,
            message: '请输入正确的手机号码',
            trigger: 'blur'
          }
        ],
        commissionRate: [
          { required: true, message: '佣金比例不能为空', trigger: 'change' }
        ]
      }
    }
  },
  created() {
    this.getList()
  },
  methods: {
    async getList() {
      this.loading = true
      try {
        const response = await listAgents(this.queryParams)
        this.agentList = response.rows || []
        this.total = Number(response.total || 0)
      } finally {
        this.loading = false
      }
    },
    handleQuery() {
      this.queryParams.pageNum = 1
      this.getList()
    },
    handleCurrentChange(page) {
      this.queryParams.pageNum = page
      this.getList()
    },
    handleSizeChange(size) {
      this.queryParams.pageSize = size
      this.queryParams.pageNum = 1
      this.getList()
    },
    handleAdd() {
      this.form = createDefaultForm()
      this.open = true
      this.$nextTick(() => {
        this.$refs.form && this.$refs.form.clearValidate()
      })
    },
    handleCancel() {
      this.open = false
      this.form = createDefaultForm()
    },
    submitForm() {
      this.$refs.form.validate(valid => {
        if (!valid) {
          return
        }
        addAgent(this.form).then(() => {
          this.open = false
          this.queryParams.pageNum = 1
          this.$message.success('新建代理成功')
          this.getList()
        })
      })
    },
    handleInlineUpdate(row, field) {
      const previousValue = row[field]
      const value = field === 'commissionRate' ? Number(row[field]) : String(row[field] || '').trim()
      if (field === 'commissionRate' && (Number.isNaN(value) || value < 0 || value > this.maxCommissionRate)) {
        row[field] = previousValue
        this.$message.error(`分成比例必须介于0和${this.maxCommissionRate}之间`)
        return
      }
      updateAgent({ agentId: row.agentId, [field]: value }).then(() => {
        row[field] = value
        this.$message.success('保存成功')
      }).catch(() => {
        row[field] = previousValue
      })
    },
    formatMoney(value) {
      return Number(value || 0).toFixed(2)
    }
  }
}
</script>

<style lang="scss" scoped>
.agent-manage-page {
  .page-card {
    border: 0;
    border-radius: 0;
    box-shadow: none;
    background: transparent;
    ::v-deep .el-card__body {
      padding: 0;
    }
  }

  .toolbar {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 16px;
    margin-bottom: 10px;
  }

  .toolbar-search {
    width: 260px;
    max-width: 100%;
  }

  .agent-table {
    width: 100%;
  }

  .agent-table ::v-deep .el-table__header th {
    background: #f7f9fc;
    color: #657083;
    font-weight: 600;
  }

  .agent-table ::v-deep .el-table__cell {
    height: 54px;
    padding: 0;
  }

  .agent-table ::v-deep .el-input--mini .el-input__inner {
    height: 30px;
    line-height: 30px;
  }

  .agent-table ::v-deep .el-input__inner {
    border-radius: 4px;
  }

  .table-footer {
    display: flex;
    align-items: center;
    justify-content: flex-end;
    gap: 16px;
    padding: 18px 0 0;
  }

  .agent-form {
    padding-right: 12px;
  }

  .commission-field {
    display: flex;
    align-items: center;
    gap: 12px;
    flex-wrap: wrap;
  }

  .commission-tip {
    color: #f56c6c;
    font-size: 13px;
  }

}
</style>
