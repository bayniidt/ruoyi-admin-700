<template>
  <el-form ref="form" :model="form" :rules="rules" label-width="140px">
    <el-form-item label="用户昵称" prop="nickName">
      <el-input v-model="form.nickName" maxlength="30" />
    </el-form-item> 
    <el-form-item label="手机号码" prop="phonenumber">
      <el-input v-model="form.phonenumber" maxlength="11" />
    </el-form-item>
    <el-form-item label="邮箱" prop="email">
      <el-input v-model="form.email" maxlength="50" />
    </el-form-item>
    <el-form-item label="PartnerStack Key" prop="partnerStackKey">
      <el-input
        v-model.trim="form.partnerStackKey"
        maxlength="100"
        show-word-limit
        placeholder="请输入 PartnerStack Key"
      />
    </el-form-item>
    <el-form-item v-if="form.isAdmin" label="推广链接设置" prop="promoBaseLink">
      <el-input
        v-model.trim="form.promoBaseLink"
        maxlength="500"
        show-word-limit
        placeholder="请输入推广基础链接，例如 https://getstartedtiktok.partnerlinks.io/vsj2hf8hifmg"
      />
    </el-form-item>
    <el-form-item label="性别">
      <el-radio-group v-model="form.sex">
        <el-radio label="0">男</el-radio>
        <el-radio label="1">女</el-radio>
      </el-radio-group>
    </el-form-item>
    <el-form-item>
      <el-button type="primary" size="mini" @click="submit">保存</el-button>
      <el-button type="danger" size="mini" @click="close">关闭</el-button>
    </el-form-item>
  </el-form>
</template>

<script>
import { updatePromoBaseLink, updateUserProfile } from "@/api/system/user"

export default {
  props: {
    user: {
      type: Object
    }
  },
  data() {
    return {
      form: {},
      // 表单校验
      rules: {
        nickName: [
          { required: true, message: "用户昵称不能为空", trigger: "blur" }
        ],
        email: [
          { required: true, message: "邮箱地址不能为空", trigger: "blur" },
          {
            type: "email",
            message: "请输入正确的邮箱地址",
            trigger: ["blur", "change"]
          }
        ],
        phonenumber: [
          { required: true, message: "手机号码不能为空", trigger: "blur" },
          {
            pattern: /^1[3|4|5|6|7|8|9][0-9]\d{8}$/,
            message: "请输入正确的手机号码",
            trigger: "blur"
          }
        ],
        partnerStackKey: [
          { required: true, message: "PartnerStack Key不能为空", trigger: "blur" },
          { max: 100, message: "PartnerStack Key长度不能超过100个字符", trigger: "blur" }
        ],
        promoBaseLink: [
          { max: 500, message: "推广链接长度不能超过500个字符", trigger: "blur" },
          {
            validator: (rule, value, callback) => {
              if (!this.form.isAdmin) {
                callback()
                return
              }
              if (!value) {
                callback(new Error("推广链接不能为空"))
                return
              }
              if (!/^https?:\/\//.test(value)) {
                callback(new Error("请输入正确的推广链接"))
                return
              }
              callback()
            },
            trigger: "blur"
          }
        ]
      }
    }
  },
  watch: {
    user: {
      handler(user) {
        if (user) {
          this.form = {
            nickName: user.nickName,
            phonenumber: user.phonenumber,
            email: user.email,
            sex: user.sex,
            partnerStackKey: user.partnerStackKey || "",
            promoBaseLink: user.promoBaseLink || "",
            isAdmin: !!user.isAdmin
          }
        }
      },
      immediate: true
    }
  },
  methods: {
    submit() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          const requests = [updateUserProfile(this.form)]
          if (this.form.isAdmin) {
            requests.push(updatePromoBaseLink(this.form.promoBaseLink))
          }
          Promise.all(requests).then(() => {
            this.$modal.msgSuccess("修改成功")
            this.user.phonenumber = this.form.phonenumber
            this.user.email = this.form.email
            this.user.partnerStackKey = this.form.partnerStackKey
            this.user.promoBaseLink = this.form.promoBaseLink
          })
        }
      })
    },
    close() {
      this.$tab.closePage()
    }
  }
}
</script>
