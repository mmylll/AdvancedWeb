<template>
  <div class="about">
    <div class="manu">
      <div class="left-image"></div>
      <router-link to="/room">
        <span class="web3d">汉诺塔</span>
      </router-link>
      <router-link to="/about">
        <span class="message">个人信息</span>
      </router-link>
      <span class="exit" @click="admin" v-if="this.$store.state.username === 'administer'">管理员</span>
      <router-link to="/">
        <span class="exit">退出</span>
      </router-link>
      <el-dialog
              v-model="quitDialog"
              title="退出"
              width="30%"
              center
              id="quit">
        <div>
          <el-button @click="quit">确定</el-button>
          <el-button @click="quitDialog=false">取消</el-button>
        </div>
      </el-dialog>
    </div>
    <div class="info">
      <h2>个人信息展示</h2>
      <div class="head">
        <div class="img" id="scene">
        </div>
        <div class="username">
        <ul>
          <li>
            <div class="title">用户名：</div>
            <input v-model="username" disabled>
          </li>
          <li>
            <div class="title">邮箱：</div>
            <input placeholder="31231@qq.com" disabled>
          </li>
        </ul>
      </div>
      </div>

      <div class="log">
        <h2>操作记录：</h2>
        <p v-for="(log, index) in logs" :id="index">操作类型： {{log.type}}   操作记录： {{log.date}}</p>
      </div>
    </div>
  </div>
</template>
<script>
import Base3d from "../../public/js/showRobot";
export default {
  name: 'About',
  data() {
    return {
      username: this.$store.state.username,
      logs: [],
      robot3D: {},
      quitDialog: false
    }
  },
  methods: {
    getLog(){
      this.axios.get('/Log?username='+this.$store.state.username).then((res) => {
        this.logs = res.data.data;
      })
    },
    admin(){
      this.quitDialog = true;
    }
  },
  mounted() {
    this.getLog();
    this.robot3D = new Base3d('#scene');
  }
}
</script>
<style>
.about {
  color: #2c3e50;
  background-image: url("../assets/skyBox/back.jpg");
  background-size: 100%;
  opacity: 1;
}

.manu {
  width: 100%;
  height: 3.4rem;
  background-color: #2c3e50;
  text-align: center;
}

span {
  color: white;
  display: block;
  float: left;
  margin: 0 auto;
  padding: 1rem;
}

span:hover {
  cursor: default;
}

span:hover {
  background-position: 0 -30px;
  background-color: #999999;
}

.web3d {
  margin-left: 15%;
}

.message {
  border: chocolate solid 1px;
  opacity: 97%;
  border-radius: 10px;
  color: darkorange;
}

.info {
  width: 70%;
  margin: 0 auto;
  border: #2c3e50 solid 1px;
  background-image: none;
  background-color: white;
}

.head {
  align-content: center;
  width: 100%;
  height: 20rem;
  border: darkblue solid 1px;
  border-radius: 10px;
}

.img {
  display: block;
  border: #2c3e50 solid 1px;
  width: 13rem;
  height: 13rem;
  float: left;
  margin: 3rem 3rem 3rem 7rem;

}

.intro {
  margin: 3rem 3rem 3rem 0rem;
  display: block;
  float: left;
  width: 30rem;
  height: 13rem;
  border: #2c3e50 solid 1px;
}

li {
  display: block;
  width: 90%;
  font-size: 1rem;
}

.title {
  text-align: left;
  font-family: "Arial Black";
}

input {
  width: 100%;
  font-size: 1rem;
}

.game {
  border-radius: 10px;
  height: 20rem;
  border: #2c3e50 solid 1px;
}
</style>


