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
                <input class="Username" name="Username" style="margin-bottom: 0px" placeholder="用户名" v-check="{num:0,that}" v-model="loginForm.Username" type="text"/>
                <p class="Username_p messageP" :style="{'color': colors[0]}">请填写用户名！</p>
                <input class="Password" name="Password" v-check="{num:1, that}" style="margin-top: 0px;margin-bottom: 0px" placeholder="密码" v-model="loginForm.Password" type="Password"/>
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
                <input v-check="{num:2,that}"  name="email" style="margin-bottom: 0px" placeholder="邮箱" v-model="registerForm.email" type="text"/>
                <p class="email_p messageP" :style="{'color': colors[2]}">请填写邮箱！</p>
                <input v-check="{num:3,that}" name="fullName" style="margin-top: 0px;margin-bottom: 0px" placeholder="用户名" v-model="registerForm.fullName" type="text"/>
                <p class="fullName_p messageP" :style="{'color': colors[3]}">请填写用户名！</p>
                <input v-check="{num:4,that}" name="Username" style="margin-top: 0px;margin-bottom: 0px" placeholder="名字" v-model="registerForm.Username" type="text"/>
                <p class="Username_p2 messageP" :style="{'color': colors[4]}">请填写名字！</p>
                <input v-check="{num:5,that}" name="Password" style="margin-top: 0px;margin-bottom: 0px" placeholder="密码" v-model="registerForm.Password" type="Password"/>
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
import Message from '../components/messageJS.js'
export default {
  // eslint-disable-next-line vue/multi-word-component-names
  name: 'Log',
  data() {
    return {
      loginForm:{
        Username:'',
        Password:''
      },
      registerForm:{
        email:'',
        fullName:'',
        Username: '',
        Password: ''
      },
      colors:['white','white','white','white','white','white',],
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
    toggleClass() {
      this.isLogIn = !this.isLogIn
    },
    checkLogin() {
      if (this.loginForm.Password == ''||this.loginForm.Username == "") {
        return false;
      }
      return true;
    },
    checkRegister() {
      if (this.registerForm.Password == ''||this.registerForm.Username == ""||this.registerForm.email==''||this.registerForm.fullName=='') {
        return false;
      }
      return true;
    },
    login() {
      if (this.checkLogin()) {
        let postData = new URLSearchParams();
        postData.append('username',this.loginForm.Username);
        postData.append('password',this.loginForm.Password);
        console.log(this.loginForm)
        this.axios.post('/Login', postData)
          .then(resp => {
            if (resp.status === 200) {
              Message({ text: '登录成功', type: 'success' })
              this.$router.replace('/About')
            } else {
              Message({ text: '登录失败，账号出错', type: 'error' })
            }
          })
          .catch(error => {
            console.log(error)
            Message({ text: '登录失败，账号出错', type: 'error' })
          })

        // 测试用，无需检验 登录直接跳转到主页面
        //this.$router.replace('/home')
      } else {
        Message({ text: '登录信息不完整', type: 'error' })
      }
    },
    register(){
       if (this.checkRegister()){
        this.axios.post('/Register', {
          username: this.registerForm.Username,
          password: this.registerForm.Password,
          email: this.registerForm.email
        })
          .then(resp => {
            console.log(resp)
            // 根据后端的返回数据修改
            if (resp.status === 200) {
              // 跳转到login
              Message({ text: '注册成功', type: 'success' })
            this.toggleClass()
            } else {
              Message({ text: '注册失败', type: 'error' })
            }
          })
          .catch(error => {
            console.log(error)
            Message({ text: '注册失败', type: 'error' })
          })

      }else {
        Message({ text: '注册信息不完整', type: 'error' })
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
