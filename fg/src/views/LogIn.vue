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
                <input name="Username" placeholder="用户名" type="text"/>
                <input name="Password" placeholder="密码" type="Password"/>
                <div class="btn">
                  登录
                </div>
              </div>
            </div>
          </div>
          <div class="form-item sign-up">
            <div class="table">
              <div class="table-cell">
                <input name="email" placeholder="邮箱" type="text"/>
                <input name="fullName" placeholder="用户名" type="text"/>
                <input name="Username" placeholder="名字" type="text"/>
                <input name="Password" placeholder="密码" type="Password"/>
                <div class="btn">
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
export default {
  name: "Log",
  data() {
    return {
      isLogIn: false
    }
  },
  methods: {
    toggleClass() {
      this.isLogIn = !this.isLogIn
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