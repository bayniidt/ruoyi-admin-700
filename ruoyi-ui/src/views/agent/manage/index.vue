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

      <el-table :data="pagedList" border class="agent-table">
        <el-table-column prop="userName" label="用户名" min-width="160" />
        <el-table-column prop="nickName" label="昵称" min-width="160" />
        <el-table-column prop="remark" label="备注" min-width="180">
          <template slot-scope="scope">
            <span>{{ scope.row.remark || "-" }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="commissionRate" label="分成比例" min-width="140" align="center">
          <template slot-scope="scope">
            <span>{{ scope.row.commissionRate }}%</span>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" min-width="200" align="center" />
      </el-table>

      <div class="table-footer">
        <div class="table-total">共 {{ filteredList.length }} 条</div>
        <el-pagination
          background
          layout="sizes, prev, pager, next"
          :current-page.sync="queryParams.pageNum"
          :page-size.sync="queryParams.pageSize"
          :page-sizes="[20, 30, 50]"
          :total="filteredList.length"
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
          <el-input v-model="form.password" type="password" placeholder="请输入用户密码" show-password maxlength="20" />
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
const mockAgents = [
  {
    id: 1,
    userName: '1320',
    nickName: 'PK',
    remark: '',
    commissionRate: 20,
    createTime: '2026-07-13 10:54:16'
  },
  {
    id: 2,
    userName: 'MY2026',
    nickName: 'MY Team',
    remark: '重点渠道代理',
    commissionRate: 18,
    createTime: '2026-07-12 15:20:08'
  }
]

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
      maxCommissionRate: 20,
      queryParams: {
        keyword: '',
        pageNum: 1,
        pageSize: 20
      },
      agentList: mockAgents,
      form: createDefaultForm(),
      rules: {
        userName: [
          { required: true, message: '用户名不能为空', trigger: 'blur' },
          { min: 2, max: 20, message: '用户名长度必须介于 2 和 20 之间', trigger: 'blur' }
        ],
        password: [
          { required: true, message: '用户密码不能为空', trigger: 'blur' },
          { min: 6, max: 20, message: '用户密码长度必须介于 6 和 20 之间', trigger: 'blur' }
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
            pattern: /^1[3-9]\\d{9}$/,
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
  computed: {
    filteredList() {
      const keyword = this.queryParams.keyword.trim().toLowerCase()
      if (!keyword) {
        return this.agentList
      }
      return this.agentList.filter(item => item.userName.toLowerCase().includes(keyword))
    },
    pagedList() {
      const start = (this.queryParams.pageNum - 1) * this.queryParams.pageSize
      const end = start + this.queryParams.pageSize
      return this.filteredList.slice(start, end)
    }
  },
  methods: {
    handleQuery() {
      this.queryParams.pageNum = 1
    },
    handleCurrentChange(page) {
      this.queryParams.pageNum = page
    },
    handleSizeChange(size) {
      this.queryParams.pageSize = size
      this.queryParams.pageNum = 1
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
        const newAgent = {
          id: Date.now(),
          userName: this.form.userName,
          nickName: this.form.nickName,
          remark: this.form.remark,
          commissionRate: this.form.commissionRate,
          createTime: this.parseCurrentTime()
        }
        this.agentList = [newAgent, ...this.agentList]
        this.open = false
        this.queryParams.pageNum = 1
        this.$message.success('新增代理成功（Mock）')
      })
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
.agent-manage-page {
  .page-card {
    border-radius: 18px;
  }

  .toolbar {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 16px;
    margin-bottom: 18px;
  }

  .toolbar-search {
    width: 260px;
    max-width: 100%;
  }

  .agent-table ::v-deep .el-table__header th {
    background: #f7f9fc;
    color: #5d6785;
    font-weight: 600;
  }

  .table-footer {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 16px;
    padding-top: 18px;
  }

  .table-total {
    color: #7b849d;
    font-size: 13px;
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
