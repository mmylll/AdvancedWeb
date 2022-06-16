<template>
  <div class="about">
    <el-menu
        default-active="2"
        class="el-menu-demo"
        mode="horizontal"
        @select="handleSelect"
    >
      <el-menu-item index="1">汉诺塔</el-menu-item>
      <el-menu-item index="2">个人信息</el-menu-item>
      <el-menu-item index="3" v-if="this.$store.state.username === 'administer'">设置参数</el-menu-item>
      <el-menu-item index="4">退出</el-menu-item>
    </el-menu>

    <el-dialog
        v-model="dialogVisible"
        title="设置参数"
        width="30%">
      <h2>设置参数</h2>
      <el-input v-model="plateNumber" placeholder="输入参数"/>
      <template #footer>
      <span class="dialog-footer">
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="setPlateNumber"
        >确定</el-button>
      </span>
      </template>
    </el-dialog>

    <div class="info">
      <h2>个人信息</h2>
      <el-container>
        <el-aside width="50%">
          <div class="img" id="scene">
          </div>
        </el-aside>
        <el-main>
          <el-card class="box-card">
            <template #header>
              <div class="card-header">
                <span>个人信息</span>
              </div>
            </template>
            <div class="text item"><strong>用户名：</strong>{{ username }}</div>
            <div class="text item"><strong>邮箱：</strong>{{ email }}</div>
          </el-card>
        </el-main>
      </el-container>
    </div>


    <div class="log">
      <h2>操作记录</h2>
      <el-table :data="logs" stripe style="width: 100%" height="350px">
        <el-table-column prop="username" label="用户"/>
        <el-table-column prop='type' label="操作类型"
                         :filters="[
              {text: '加入', value: 'join'},
              {text: '离开', value: 'leave'},
              {text: '拿起', value: 'pickUp'},
              {text: '放下', value: 'putDown'}
          ]"
                         :filter-method="filterType"/>
        <el-table-column prop='date' label="时间"/>
        <el-table-column prop="plate" label="操作圆盘对象"
                         :filters="plate_filter" :formatter="format"
                         :filter-method="filterPlate"/>
      </el-table>
      <!--      <p v-for="(log, index) in logs" :id="index">操作类型： {{ log.type }} 操作记录： {{ log.date }}</p>-->
    </div>

    <div class="echarts">
      <div class="e-item" id="e-one"></div>
      <div class="e-item" id="e-two"></div>
    </div>
  </div>
</template>
<script>
import Base3d from "../../public/js/showRobot";
import qs from "qs";
import * as echarts from 'echarts'

export default {
  name: 'About',
  data() {
    return {
      username: this.$store.state.username,
      email: '',
      logs: [],
      robot3D: {},
      dialogVisible: false,
      plateNumber: 3,
      plate_filter: [
        {text: 'null', value: null},
        {text: '0', value: 0},
        {text: '1', value: 1},
        {text: '2', value: 2}
      ],
      optionOne: {
        title: {
          text: '操作记录条形图'
        },
        tooltip: {},
        legend: {
          data: ['次数']
        },
        xAxis: {
          data: ["join", "leave", 'pickUp', "putDown"]
        },
        yAxis: {},
        series: [{
          name: '次数',
          type: 'bar',
          width: '20',
          data: [0, 0, 0, 0]
        }]
      },
      optionTwo: {
        title: {
          text: '操作记录饼状图'
        },
        tooltip: {},
        series: [{
          name: '次数',
          type: 'pie',
          radius: '55%',
          data: [{value: 0, name: 'join'}, {value: 0, name: 'leave'}, {value: 0, name: 'pickUp'}, {
            value: 0,
            name: 'putDown'
          }]
        }]
      }
    }
  },
  methods: {
    getLog() {
      this.axios.get('/Log?username=' + this.$store.state.username).then((res) => {
        for (let log of res.data.data) {
          log.date = log.date[0] + '-' + log.date[1] + '-' + log.date[2] + ' ' + log.date[3] + ':' + log.date[4] + ':' + log.date[5]
          this.logs.push(log)
        }
        this.count()
      })
    },
    setPlateNumber() {
      this.axios.post('/Set', qs.stringify({number: Number.parseInt(this.plateNumber)})).then(() => {
        this.$message({
          type: 'success',
          message: '设置成功'
        })
        this.dialogVisible = false
        this.plate_filter = []
        for (let i in this.plateNumber) {
          this.plate_filter.push({text: '' + i, value: i})
        }
        this.plate_filter.push({text: 'null', value: null})
      }).catch(() => {
        this.$message({
          type: 'warning',
          message: '设置失败，有玩家在房间内'
        })
      })
    },
    handleSelect(key) {
      if (key === '1') {
        this.$router.replace('/room')
      } else if (key === '2') {
        this.$router.replace('/About')
      } else if (key === '3') {
        this.dialogVisible = true;
      } else {
        localStorage.clear();
        this.$store.state.username = null;
        this.$store.state.token = null
        this.$router.replace('/')
      }
    },
    getInfo() {
      this.axios.get('/Info?username=' + this.username).then((res) => {
        this.email = res.data.data.email
      })
    },
    format(row, column, cellValue, index) {
      if (cellValue === null)
        return 'null';
      return cellValue;
    },
    filterType(value, row) {
      return row.type === value;
    },
    filterPlate(value, row) {
      return row.plate === value;
    },

    count() {

      let map = {
        'join': 0,
        'leave': 1,
        'pickUp': 2,
        'putDown': 3
      }
      for (let log of this.logs) {
        this.optionOne.series[0].data[map[log.type]]++;
        this.optionTwo.series[0].data[map[log.type]].value++;
      }
      this.paintCharts()
      // let chart1 = echarts.init(document.getElementById('e-one'))
      // chart1.setOption(this.optionOne)
      //
      // console.log(this.optionOne)
    },
    paintCharts() {
      let chart1 = echarts.init(document.getElementById('e-one'), null, {
        width: 600,
        height: 400
      })
      chart1.setOption(this.optionOne)
      let chart2 = echarts.init(document.getElementById('e-two'))
      chart2.setOption(this.optionTwo)
    }
  },
  mounted() {
    this.getInfo();
    this.getLog();
    this.paintCharts();
    this.robot3D = new Base3d('#scene');
  }
}
</script>
<style>
.info {
  width: 60%;
  margin: 10px auto;
  border: #2c3e50 solid 2px;
  border-radius: 8px;
}

.item {
  text-align: left;
  margin: 20px auto;
}

.box-card {
  height: 90%;
}

.log {
  width: 60%;
  margin: 10px auto;
  padding: 10px;
  border: #2c3e50 solid 2px;
  border-radius: 8px;
}

.e-item {
  width: 45%;
  height: 400px;
}

.echarts {
  width: 65%;
  display: flex;
  padding: 10px;
  margin: 10px auto;
  justify-content: space-between;
  border: #2c3e50 solid 2px;
  border-radius: 8px;
}

/*.about {*/
/*  color: #2c3e50;*/
/*  background-image: url("../assets/skyBox/back.jpg");*/
/*  background-size: 100%;*/
/*  opacity: 1;*/
/*}*/

/*.manu {*/
/*  width: 100%;*/
/*  height: 3.4rem;*/
/*  background-color: #2c3e50;*/
/*  text-align: center;*/
/*}*/

/*!*span {*!*/
/*!*  color: white;*!*/
/*!*  display: block;*!*/
/*!*  float: left;*!*/
/*!*  margin: 0 auto;*!*/
/*!*  padding: 1rem;*!*/
/*!*}*!*/

/*span:hover {*/
/*  cursor: default;*/
/*}*/

/*!*span:hover {*!*/
/*!*  background-position: 0 -30px;*!*/
/*!*  background-color: #999999;*!*/
/*!*}*!*/

/*.web3d {*/
/*  margin-left: 15%;*/
/*}*/

/*.message {*/
/*  border: chocolate solid 1px;*/
/*  opacity: 97%;*/
/*  border-radius: 10px;*/
/*  color: darkorange;*/
/*}*/

/*.info {*/
/*  width: 70%;*/
/*  margin: 0 auto;*/
/*  border: #2c3e50 solid 1px;*/
/*  background-image: none;*/
/*  background-color: white;*/
/*}*/

/*.head {*/
/*  align-content: center;*/
/*  width: 100%;*/
/*  height: 20rem;*/
/*  border: darkblue solid 1px;*/
/*  border-radius: 10px;*/
/*}*/

.img {
  /*border: #2c3e50 solid 1px;*/
  width: 300px;
  height: 300px;
  margin: 10px auto;
  /*float: left;*/
  /*margin: 3rem 3rem 3rem 7rem;*/
}

/*.intro {*/
/*  margin: 3rem 3rem 3rem 0rem;*/
/*  display: block;*/
/*  float: left;*/
/*  width: 30rem;*/
/*  height: 13rem;*/
/*  border: #2c3e50 solid 1px;*/
/*}*/

/*!*li {*!*/
/*!*  display: block;*!*/
/*!*  width: 90%;*!*/
/*!*  font-size: 1rem;*!*/
/*!*}*!*/

/*.title {*/
/*  text-align: left;*/
/*  font-family: "Arial Black";*/
/*}*/

/*input {*/
/*  width: 100%;*/
/*  font-size: 1rem;*/
/*}*/

/*.game {*/
/*  border-radius: 10px;*/
/*  height: 20rem;*/
/*  border: #2c3e50 solid 1px;*/
/*}*/
</style>


