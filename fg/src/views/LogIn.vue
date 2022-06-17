<template>
  <div class="log-in-main">
    <div :class="{'container':true,'log-in':isLogIn}">
      <div class="box"></div>
      <div class="container-forms">
        <div class="container-info">
          <div class="info-item">
            <div class="table">
              <div class="table-cell">
                <p>
                  已有账号?
                </p>
                <div class="btn" @click="toggleClass">
                  登录
                </div>
              </div>
            </div>
          </div>
          <div class="info-item">
            <div class="table">
              <div class="table-cell">
                <p>
                  还没有账号?
                </p>
                <div class="btn" @click="toggleClass">
                  注册
                </div>
              </div>
            </div>
          </div>
        </div>
        <div class="container-form">
          <div class="form-item log-in">
            <div class="table">
              <div class="table-cell">
                <input class="Username" name="Username" style="margin-bottom: 0px" placeholder="用户名"
                       v-check="{num:0,that}" v-model="loginForm.Username" type="text"/>
                <p class="Username_p messageP" :style="{'color': colors[0]}">请填写用户名！</p>
                <input class="Password" name="Password" v-check="{num:1, that}"
                       style="margin-top: 0px;margin-bottom: 0px" placeholder="密码" v-model="loginForm.Password"
                       type="Password"/>
                <p class="Password_p messageP" :style="{'color': colors[1]}">请填写密码！</p>
                <div class="btn" @click="this.login">
                  登录
                </div>
              </div>
            </div>
          </div>
          <div class="form-item sign-up">
            <div class="table">
              <div class="table-cell">
                <input v-check="{num:2,that}" name="email" style="margin-bottom: 0px" placeholder="邮箱"
                       v-model="registerForm.email" type="text"/>
                <p class="email_p messageP" :style="{'color': colors[2]}">请填写邮箱！</p>
                <input v-check="{num:3,that}" name="fullName" style="margin-top: 0px;margin-bottom: 0px"
                       placeholder="用户名" v-model="registerForm.Username" type="text"/>
                <p class="fullName_p messageP" :style="{'color': colors[3]}">请填写用户名！</p>
                <!--                <input v-check="{num:4,that}" name="Username" style="margin-top: 0px;margin-bottom: 0px"-->
                <!--                       placeholder="名字" v-model="registerForm.Username" type="text"/>-->
                <!--                <p class="Username_p2 messageP" :style="{'color': colors[4]}">请填写名字！</p>-->
                <input v-check="{num:5,that}" name="Password" style="margin-top: 0px;margin-bottom: 0px"
                       placeholder="密码" v-model="registerForm.Password" type="Password"/>
                <p class="Password_p2 messageP" :style="{'color': colors[5]}">请填写密码！</p>
                <div class="btn" @click="this.register">
                  注册
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>


</template>

<script>
  import {ElMessage} from "element-plus"
  import Qs from 'qs'

  export default {
    // eslint-disable-next-line vue/multi-word-component-names
    name: 'Log',
    data() {
      return {
        loginForm: {
          Username: '',
          Password: ''
        },
        registerForm: {
          email: '',
          Username: '',
          Password: ''
        },
        colors: ['white', 'white', 'white', 'white', 'white', 'white',],
        isLogIn: false,
        that: this
      }
    },
    directives: {
      // 指令名称
      'check': (el, binding) => {
        var temp = binding.value.num;
        var that = binding.value.that;
        // input 添加失焦时间
        el.addEventListener('blur', (e) => {
          // 未匹配成功
          if (e.target.value == '') {
            console.log(that.colors[temp]);
            that.colors[temp] = 'red';
            el.style.borderColor = '#f00'
          } else {
            that.colors[temp] = 'white';
            el.style.borderColor = '#000'
          }
        })
      }
    },
    methods: {
      // 是在登录还是注册
      toggleClass() {
        this.isLogIn = !this.isLogIn
      },
      // 检测登录的信息是否为空
      checkLogin() {
        if (this.loginForm.Password === '' || this.loginForm.Username === "") {
          return false;
        }
        return true;
      },
      // 检查注册的信息是否为空
      checkRegister() {
        if (this.registerForm.Password === '' || this.registerForm.Username === "" || this.registerForm.email === '') {
          return false;
        }
        return true;
      },
      // 登录
      login() {
        if (this.checkLogin()) {
          this.axios.post('/Login', Qs.stringify({
            username: this.loginForm.Username,
            password: this.loginForm.Password,
          }))
                  .then(resp => {
                    if (resp.data.status === 200) {
                      ElMessage.success('登录成功')

                      this.$store.commit('setToken', resp.data.data)
                      this.$store.commit('setUsername', this.loginForm.Username)
                      // 登录成功跳转个人信息页面
                      this.$router.push('/About')
                    } else {
                      ElMessage.error(resp.data.message)
                    }
                  })
                  .catch(error => {
                    ElMessage.error('登录失败，账号出错');
                  })

        } else {
          ElMessage.warning("登录信息不完整")
        }
      },
      // 注册
      register() {
        if (this.checkRegister()) {
          this.axios.post('/Register', Qs.stringify({
            username: this.registerForm.Username,
            password: this.registerForm.Password,
            email: this.registerForm.email
          }))
                  .then(resp => {
                    console.log(resp)
                    // 根据后端的返回数据修改
                    if (resp.data.status === 200) {
                      // 跳转到login
                      ElMessage.success('注册成功')
                      this.toggleClass()
                    } else {
                      ElMessage.error(resp.data.message)
                    }
                  })
                  .catch(error => {
                    console.log(error)
                    ElMessage.error('注册失败')
                  })

        } else {
          ElMessage.warning("注册信息不完整")
        }
      }
    },
    mounted() {


    }
  }
</script>

<style scoped></style>
<style>
  @import "../CSS/logIn.css";
</style>
