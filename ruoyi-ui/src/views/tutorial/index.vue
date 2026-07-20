<template>
  <div class="app-container tutorial-page">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" label-width="68px">
      <el-form-item label="文件名称" prop="fileName">
        <el-input
          v-model="queryParams.fileName"
          placeholder="请输入文件名称"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8" v-if="isAdmin">
      <el-col :span="1.5">
        <el-upload
          :show-file-list="false"
          :http-request="handleUpload"
          :before-upload="beforeUpload"
          action="#"
        >
          <el-button type="primary" plain icon="el-icon-upload2" size="mini">新增文件</el-button>
        </el-upload>
      </el-col>
    </el-row>

    <el-table v-loading="loading" :data="fileList">
      <el-table-column label="文件名称" min-width="320" show-overflow-tooltip>
        <template slot-scope="scope">
          <a class="link-type" @click="handlePreview(scope.row)">{{ scope.row.fileName }}</a>
        </template>
      </el-table-column>
      <el-table-column label="类型" prop="fileType" width="120" align="center" />
      <el-table-column label="大小" width="120" align="right">
        <template slot-scope="scope">{{ formatSize(scope.row.fileSize) }}</template>
      </el-table-column>
      <el-table-column label="上传人" prop="createBy" width="120" align="center" />
      <el-table-column label="上传时间" prop="createTime" width="180" align="center">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.createTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column v-if="isAdmin" label="操作" width="120" align="center" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button
            size="mini"
            type="text"
            icon="el-icon-delete"
            @click="handleDelete(scope.row)"
          >删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination
      v-show="total>0"
      :total="total"
      :page.sync="queryParams.pageNum"
      :limit.sync="queryParams.pageSize"
      @pagination="getList"
    />
  </div>
</template>

<script>
import { mapGetters } from 'vuex'
import { listTutorialFiles, uploadTutorialFile, delTutorialFile, previewTutorialFile } from '@/api/tutorial'

export default {
  name: 'Tutorial',
  data() {
    return {
      loading: true,
      total: 0,
      fileList: [],
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        fileName: undefined
      }
    }
  },
  computed: {
    ...mapGetters(['roles']),
    isAdmin() {
      return this.roles.includes('admin')
    }
  },
  created() {
    this.getList()
  },
  methods: {
    getList() {
      this.loading = true
      listTutorialFiles(this.queryParams).then(response => {
        this.fileList = response.rows
        this.total = response.total
        this.loading = false
      }).catch(() => {
        this.loading = false
      })
    },
    handleQuery() {
      this.queryParams.pageNum = 1
      this.getList()
    },
    resetQuery() {
      this.resetForm('queryForm')
      this.handleQuery()
    },
    beforeUpload(file) {
      const maxSize = 50 * 1024 * 1024
      if (file.size > maxSize) {
        this.$modal.msgError('文件大小不能超过 50MB')
        return false
      }
      return true
    },
    handleUpload(options) {
      const formData = new FormData()
      formData.append('file', options.file)
      uploadTutorialFile(formData).then(() => {
        this.$modal.msgSuccess('上传成功')
        this.getList()
      }).catch(() => {
        options.onError()
      })
    },
    handlePreview(row) {
      previewTutorialFile(row.fileId).then(blob => {
        const url = window.URL.createObjectURL(blob)
        window.open(url, '_blank')
        setTimeout(() => window.URL.revokeObjectURL(url), 60000)
      })
    },
    handleDelete(row) {
      this.$modal.confirm('是否确认删除文件 "' + row.fileName + '"？').then(() => {
        return delTutorialFile(row.fileId)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess('删除成功')
      }).catch(() => {})
    },
    formatSize(size) {
      if (!size && size !== 0) return '-'
      if (size < 1024) return size + ' B'
      if (size < 1024 * 1024) return (size / 1024).toFixed(1) + ' KB'
      return (size / 1024 / 1024).toFixed(1) + ' MB'
    }
  }
}
</script>

<style scoped>
.tutorial-page .link-type {
  cursor: pointer;
}
</style>
