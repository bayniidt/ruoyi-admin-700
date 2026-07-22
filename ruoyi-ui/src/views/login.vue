<template>
  <div class="login">
    <div class="login-page-title">TK返点系统查询</div>
    <main class="login-panel">
      <h1 class="login-hero-title">{{ title }}</h1>
      <el-form ref="loginForm" :model="loginForm" :rules="loginRules" class="login-form">
        <el-form-item prop="username">
          <el-input
            v-model="loginForm.username"
            type="text"
            auto-complete="off"
            placeholder="账号"
          >
            <svg-icon slot="prefix" icon-class="user" class="el-input__icon input-icon" />
          </el-input>
        </el-form-item>
        <el-form-item prop="password">
          <el-input
            v-model="loginForm.password"
            type="password"
            auto-complete="off"
            placeholder="密码"
            @keyup.enter.native="handleLogin"
          >
            <svg-icon slot="prefix" icon-class="password" class="el-input__icon input-icon" />
          </el-input>
        </el-form-item>
        <el-form-item prop="code" v-if="captchaEnabled">
          <el-input
            v-model="loginForm.code"
            auto-complete="off"
            placeholder="验证码"
            style="width: 63%"
            @keyup.enter.native="handleLogin"
          >
            <svg-icon slot="prefix" icon-class="validCode" class="el-input__icon input-icon" />
          </el-input>
          <div class="login-code">
            <img :src="codeUrl" @click="getCode" class="login-code-img"/>
          </div>
        </el-form-item>
        <el-checkbox v-model="loginForm.rememberMe" style="margin:0px 0px 25px 0px;">记住密码</el-checkbox>
        <el-form-item style="width:100%;">
          <el-button
            :loading="loading"
            size="medium"
            type="primary"
            style="width:100%;"
            @click.native.prevent="handleLogin"
          >
            <span v-if="!loading">登 录</span>
            <span v-else>登 录 中...</span>
          </el-button>
          <div style="float: right;" v-if="register">
            <router-link class="link-type" :to="'/register'">立即注册</router-link>
          </div>
        </el-form-item>
      </el-form>
    </main>
    <!--  底部  -->
    <div class="el-login-footer">
      <span>{{ footerContent }}</span>
    </div>
  </div>
</template>

<script>
import Cookies from "js-cookie"
import { encrypt, decrypt } from '@/utils/jsencrypt'
import defaultSettings from '@/settings'

export default {
  name: "Login",
  data() {
    return {
      title: process.env.VUE_APP_TITLE,
      footerContent: defaultSettings.footerContent,
      codeUrl: "",
      loginForm: {
        username: "",
        password: "",
        rememberMe: false,
        code: "",
        uuid: ""
      },
      loginRules: {
        username: [
          { required: true, trigger: "blur", message: "请输入您的账号" }
        ],
        password: [
          { required: true, trigger: "blur", message: "请输入您的密码" }
        ],
        code: [{ required: true, trigger: "change", message: "请输入验证码" }]
      },
      loading: false,
      // 验证码开关
      captchaEnabled: false,
      // 注册开关
      register: false,
      redirect: undefined
    }
  },
  watch: {
    $route: {
      handler: function(route) {
        this.redirect = route.query && route.query.redirect
      },
      immediate: true
    }
  },
  created() {
    this.getCookie()
  },
  methods: {
    getCode() {
      this.captchaEnabled = false
      this.codeUrl = ""
      this.loginForm.code = ""
      this.loginForm.uuid = ""
    },
    getCookie() {
      const username = Cookies.get("username")
      const password = Cookies.get("password")
      const rememberMe = Cookies.get('rememberMe')
      this.loginForm = {
        username: username === undefined ? this.loginForm.username : username,
        password: password === undefined ? this.loginForm.password : decrypt(password),
        rememberMe: rememberMe === undefined ? false : Boolean(rememberMe)
      }
    },
    handleLogin() {
      this.$refs.loginForm.validate(valid => {
        if (valid) {
          this.loading = true
          if (this.loginForm.rememberMe) {
            Cookies.set("username", this.loginForm.username, { expires: 30 })
            Cookies.set("password", encrypt(this.loginForm.password), { expires: 30 })
            Cookies.set('rememberMe', this.loginForm.rememberMe, { expires: 30 })
          } else {
            Cookies.remove("username")
            Cookies.remove("password")
            Cookies.remove('rememberMe')
          }
          this.$store.dispatch("Login", this.loginForm).then(() => {
            this.$router.push({ path: this.redirect || "/" }).catch(()=>{})
          }).catch(() => {
            this.loading = false
          })
        }
      })
    }
  }
}
</script>

<style rel="stylesheet/scss" lang="scss" scoped>
.login {
  position: relative;
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100%;
  overflow: hidden;
  background:
    radial-gradient(circle at 52% 88%, rgba(100, 48, 197, 0.42) 0, rgba(100, 48, 197, 0.2) 16%, rgba(100, 48, 197, 0) 34%),
    linear-gradient(116deg, #020817 0%, #0b1745 34%, #073f4a 72%, #06101d 100%);
  background-size: cover;
}
.login-page-title,
.login-hero-title {
  background: linear-gradient(90deg, #1d73dc 0%, #13c7d8 47%, #7566f1 100%);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
  color: transparent;
}
.login-page-title {
  position: absolute;
  top: 72px;
  left: 42px;
  z-index: 2;
  font-size: 34px;
  line-height: 1.2;
  font-weight: 800;
  letter-spacing: 1px;
}
.login-panel {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 100%;
  gap: 40px;
  transform: translateY(-90px);
}
.login-hero-title {
  margin: 0;
  text-align: center;
  font-size: clamp(80px, 7.4vw, 142px);
  line-height: 1.05;
  font-weight: 500;
  letter-spacing: 4px;
  white-space: nowrap;
}

.login-form {
  box-sizing: border-box;
  border-radius: 8px;
  background: #ffffff;
  width: 520px;
  padding: 44px 42px 24px;
  z-index: 1;
  .el-input {
    height: 52px;
    input {
      height: 52px;
      font-size: 17px;
    }
  }
  .input-icon {
    height: 18px;
    width: 18px;
    margin-left: 4px;
  }
}
::v-deep .login-form .el-input__prefix {
  display: flex;
  align-items: center;
}
::v-deep .login-form .el-input__inner {
  height: 52px;
  line-height: 52px;
  font-size: 17px;
}
.login-tip {
  font-size: 13px;
  text-align: center;
  color: #bfbfbf;
}
.login-code {
  width: 33%;
  height: 52px;
  float: right;
  img {
    cursor: pointer;
    vertical-align: middle;
  }
}
.el-login-footer {
  height: 40px;
  line-height: 40px;
  position: fixed;
  bottom: 0;
  width: 100%;
  text-align: center;
  color: #fff;
  font-family: Arial;
  font-size: 14px;
  letter-spacing: 1px;
  z-index: 1;
}
.login-code-img {
  height: 52px;
}
::v-deep .el-checkbox__label {
  font-size: 16px;
}
::v-deep .el-button {
  height: 48px;
  font-size: 17px;
  font-weight: 700;
}
@media (max-width: 768px) {
  .login-page-title {
    top: 28px;
    left: 24px;
    font-size: 26px;
  }
  .login-form {
    width: calc(100% - 32px);
    max-width: 520px;
    padding: 36px 28px 16px;
  }
  .login-panel {
    gap: 28px;
    transform: translateY(-24px);
  }
  .login-hero-title {
    font-size: 42px;
    letter-spacing: 1px;
  }
}
</style>
