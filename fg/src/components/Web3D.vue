<template>
  <div id="main">
    <el-dialog
        v-model="quitDialog"
        title="退出"
        width="30%"
        center
        id="quit">
      <h2>是否退出</h2>
      <div>
        <el-button @click="quit">确定</el-button>
        <el-button @click="quitDialog=false">取消</el-button>
      </div>
    </el-dialog>
  </div>
</template>
<style>
#quit {
  z-index: 12000;
}
</style>
<script>
import * as THREE from 'three'
import FirstPersonControls from "../../public/js/FirstPersonControls";
import {GLTFLoader} from 'three/examples/jsm/loaders/GLTFLoader'
import {DRACOLoader} from 'three/examples/jsm/loaders/DRACOLoader'
//import socket from "@/socket";
import {getCurrentInstance} from "vue";
import io from "socket.io-client";

let scene, role, socket;
export default {
  name: "Web3D",
  data() {
    return {
      camera: null,
      render: null,
      skybox: {
        back: require('../assets/skyBox/back.jpg'),
        bottom: require('../assets/skyBox/bottom.jpg'),
        left: require('../assets/skyBox/left.jpg'),
        right: require('../assets/skyBox/right.jpg'),
        top: require('../assets/skyBox/top.jpg'),
        front: require('../assets/skyBox/front.jpg'),
      },
      texture: {
        floor: require('../assets/texture/floor.jpg')
      },
      // 退出弹窗
      quitDialog: false,

      // 角色
      player: {
        username: this.$store.state.username,
        x: 0.0,
        y: 0.0,
        z: 0.0,
        rx: 0.0,
        rz: 0.0,
        ry: 0.0,
        plate: null
      },
      //role: null,
      stateList: {},
      // actionMap: {
      //   up: {direction: 'up', rotation: Math.PI, axes: 'z'},
      //   down: {direction: 'down', rotation: 0, axes: 'z'},
      //   left: {direction: 'left', rotation: -Math.PI / 2, axes: 'x'},
      //   right: {direction: 'right', rotation: Math.PI / 2, axes: 'x'}
      // },
      // nopeAction: {direction: null},
      // nextAction: {direction: 'down', rotation: 0},
      clock: null,
      mixer: null,
      currentAction: null,
      previousAction: null,

      lastkey: '',
      firstPersonControl: null,

      otherPlayer: [],
      columns: [],
      loader: null,
      dracoLoader: null,
      animations: {
        walking: null,
        standing: null
      },
      bus: getCurrentInstance().appContext.config.globalProperties.$bus,

      pickedUpPlateObject: null, // 被拿起块的模型对象
      isPicked: false,
      originColumn: -1, // 被拿起的块的原来的柱子编号
      copyColumns: []
    }
  },
  methods: {
    init() {
      console.log("init()")
      console.log(this.columns)

      scene = new THREE.Scene()
      this.render = new THREE.WebGLRenderer({antialias: true})
      this.camera = new THREE.PerspectiveCamera(75, window.innerWidth / window.innerHeight, 0.1, 1000)
      this.camera.position.set(0, 100, 0)
      this.camera.rotation.set(0,Math.PI,0);

      this.render.setClearColor(0xffffff, 1.0)
      this.render.setSize(window.innerWidth, window.innerHeight)
      this.clock = new THREE.Clock();
      this.firstPersonControl = new FirstPersonControls(this.camera)
      this.firstPersonControl.connect()
      scene.add(this.firstPersonControl.yawObject)
      this.setBackground()
      this.addLight()
      document.getElementById('main').appendChild(this.render.domElement)
      this.renderCanvas()
    },
    quit() {
      console.log('quiting...')
      if (this.player.plate !== null) {
        console.log('plate on hand')
        this.$message({
          message: '必须放下持有的圆盘，才能离开场景',
          type: 'error'
        })
      } else {
        console.log('no plate on hand')
        socket.disconnect();
        console.log('quited')
        this.quitDialog = true;
      }
    },

    addCylinder() {
      console.log("addCylinder()")
      console.log(this.columns)

      let geometry, cylinder
      let material = new THREE.MeshBasicMaterial({color: 0xC4C2C0}),
          plateMaterial = new THREE.MeshLambertMaterial({color: 0xffff00})
      for (let i = 0; i < this.columns.length; i++) {
        let column = this.columns[i];
        geometry = new THREE.CylinderGeometry(5, 5, 100.0, 32);
        cylinder = new THREE.Mesh(geometry, material);
        scene.add(cylinder)
        cylinder.name = 'column' + i
        cylinder.position.set(100 * (i - 1), 0, -50)
        let height = 0;
        for (let plate of column.plates) {
          let plateGeometry = new THREE.CylinderGeometry(plate.radius, plate.radius, plate.height, 32);
          let plateMesh = new THREE.Mesh(plateGeometry, plateMaterial);
          scene.add(plateMesh)
          plateMesh.name = 'plate' + plate.index;
          plateMesh.position.set(...cylinder.position);
          plateMesh.position.y = height;
          height += 10.0;
        }
      }
    },

    addLight() {
      let light = new THREE.AmbientLight(' 0xaaaaaa', 1)
      scene.add(light)
    },

    setBackground() {
      const skyBoxGeometry = new THREE.BoxGeometry(500, 500, 500);
      const textureLoader = new THREE.TextureLoader();
      const skyBoxMaterial = [
        new THREE.MeshBasicMaterial({
          map: textureLoader.load(this.skybox.right), side: THREE.BackSide
        }),
        new THREE.MeshBasicMaterial({
          map: textureLoader.load(this.skybox.left), side: THREE.BackSide
        }),
        new THREE.MeshBasicMaterial({
          map: textureLoader.load(this.skybox.top), side: THREE.BackSide
        }),
        new THREE.MeshBasicMaterial({
          map: textureLoader.load(this.skybox.bottom), side: THREE.BackSide
        }),
        new THREE.MeshBasicMaterial({
          map: textureLoader.load(this.skybox.front), side: THREE.BackSide
        }),
        new THREE.MeshBasicMaterial({
          map: textureLoader.load(this.skybox.back), side: THREE.BackSide
        })
      ];
      const skyBox = new THREE.Mesh(skyBoxGeometry, skyBoxMaterial);
      scene.add(skyBox);
      textureLoader.load(this.texture.floor,
          function (texture) {
            texture.wrapS = texture.wrapT = THREE.RepeatWrapping;
            texture.repeat.set(4, 4);
            const floorMaterial = new THREE.MeshBasicMaterial({
              map: texture,
              side: THREE.DoubleSide
            });
            const floorGeometry = new THREE.PlaneGeometry(500, 500, 5, 5);
            const floor = new THREE.Mesh(floorGeometry, floorMaterial);
            floor.position.y = 0;
            floor.rotation.x = Math.PI / 2;
            scene.add(floor);
          })
    },


    renderCanvas() {
      this.render.render(scene, this.camera)
    },

    animate() {
      let dt = this.clock.getDelta()
      if (this.mixer) {
        this.mixer.update(dt)
      }

      this.firstPersonControl.update(dt);
      this.renderCanvas()
      requestAnimationFrame(() => {
        this.animate()
      })
    },
    // 创建人物
    createRole() {
      console.log("createRole()")
      console.log(this.columns)

      this.loader = new GLTFLoader()
      this.dracoLoader = new DRACOLoader()
      this.dracoLoader.setDecoderPath('/draco/')
      this.dracoLoader.preload()
      this.loader.setDRACOLoader(this.dracoLoader)

      this.loader.load('3D/RobotExpressive.glb', gltf => {
        this.role = gltf.scene
        this.role.position.y = 0
        this.role.scale.set(7, 7, 7)// 设置模型大小
        this.role.rotation.y = Math.PI
        this.mixer = new THREE.AnimationMixer(this.role);
        this.animations.walking = gltf.animations[10]
        this.animations.standing = gltf.animations[8]
        this.stateList.Walking = this.mixer.clipAction(gltf.animations[10]);
        this.stateList.Standing = this.mixer.clipAction(gltf.animations[8]);
        // 设置下面两项主要是站着的时候，别抖了
        this.stateList.Standing.clampWhenFinished = true;
        this.stateList.Standing.loop = THREE.LoopOnce;
        this.currentAction = this.stateList.Standing;
        this.currentAction.play();
        scene.add(this.role);
        this.firstPersonControl.role = this.role
        this.updatePositionAndRotation()
        console.log(this.role)

      }, undefined, function (e) {
        console.error(e);
      });
    },
    keyPressed(event) {
      if (event.keyCode === 81) {
        this.quit()
        console.log(this.quitDialog)
      } else if (event.keyCode === 32) { // 当按下空格
        if (!this.isPicked) { // 还没有人拿汉诺塔
          this.pickUp();
        }
      } else if (event.keyCode === 87 || event.keyCode === 83 || event.keyCode === 65 || event.keyCode === 68 || this.lastkey !== event.keyCode) {
        this.lastkey = event.keyCode;
        this.fadeToAction('Walking', 0.2);
      }
    },
    keyUp(event) {
      if (event.keyCode === 32) {
        if (this.isPicked && this.player.plate !== null) {
          this.putDown();
        }
      } else if (event.keyCode !== 81) {
        this.lastkey = null;
        this.nextAction = this.nopeAction;
        this.fadeToAction('Standing', 0.2);
      }
    },

    fadeToAction(name, duration) {
      this.previousAction = this.currentAction;
      this.currentAction = this.stateList[name];
      if (this.previousAction !== this.currentAction) {
        this.previousAction.fadeOut(duration);
        this.currentAction.play();
      }
      if (this.currentAction) {
        this.currentAction
            .reset()
            .setEffectiveTimeScale(1)
            .setEffectiveWeight(1)
            .fadeIn(duration)
            .play();
      }
    },
    renderOtherPlayer() {
      console.log("renderOtherPlayer()")
      console.log(this.columns)

      for (let player of this.otherPlayer) {
        this.renderAPlayer(player);
      }
    },
    renderAPlayer(player) {
      this.loader.load('3D/RobotExpressive.glb', gltf => {
        let role = gltf.scene;
        role.scale.set(7, 7, 7)// 设置模型大小
        role.rotation.set(player.rx, player.ry, player.rz);
        role.position.set(player.x, player.y, player.z)
        scene.add(role);
        let mixer = new THREE.AnimationMixer(role);
        mixer.clipAction(this.animations.standing).play();
        role.name = player.username
      })
    },
    getRoomInfo() {
      console.log("getRoomInfo()")
      console.log(this.columns)

      this.axios.get('/Join').then((res) => {
        this.columns = this.deepClone(res.data.data.columns);
        //this.columns = res.data.data.columns;
        console.log('getroominfo:  columns')
        console.log(this.columns)
        for (let i in res.data.data.players) {
          this.otherPlayer.push(res.data.data.players[i]);
        }
        this.addCylinder();
        this.renderOtherPlayer();
      })
    },
    getPlayerByName(username) {
      for (let player of this.otherPlayer) {
        if (player.username === username)
          return player;
      }
      return null
    },
    socketInit() {
      console.log("socketInit()")
      console.log(this.columns)

      //io.set('transports', ['websocket', 'xhr-polling', 'jsonp-polling', 'htmlfile', 'flashsocket']);
      //io.set('origins', '*:*');
      // socket = io.connect()
      socket = io.connect('ws://localhost:10246/', {
        autoConnect: false
        //withCredentials: false,
        //query: {username: 'administer'}
        //extraHeaders: {
        //   'Sec-WebSocket-Version': 13,
        //   'Connection': 'Upgrade',
        //   'Upgrade': 'websocket',
        //   'Sec-WebSocket-Key': '<calculated at runtime>',
        //   'Host': '<calculated at runtime>'
        // }
      });
      socket.connect();
      socket.on('connect', () => {
        console.log(socket.connected)
        this.join();
      })
      socket.on('disconnect', () => {
        this.$message({
          message: "退出成功",
          type: 'success'
        })
      })
      //socket = io('ws://127.0.0.1:10246', {withCredentials: false, query: {username: 'administer'}})

      socket.on('OnPlayerJoin', (player) => {
        console.log("onPlayerJoin")
        console.log(this.columns)

        this.otherPlayer.push(player.player)
        //console.log(player)
        this.renderAPlayer(player.player);
        this.$message({
          message: player.player.username + "加入了游戏",
          type: 'success'
        })
      })
      socket.on('OnPlayerLeave', (res) => {
        let username = res.username;
        let player = this.getPlayerByName(username);
        if (player !== null) {
          this.otherPlayer.splice(this.otherPlayer.indexOf(player), 1);
          let object = scene.getObjectByName(username);
          scene.remove(object);
          this.$message({
            message: username + "离开了游戏",
            type: 'success'
          })
        }
      })
      socket.on('OnPlayerUpdate', (res) => {
        let player = res.player;
        let object = scene.getObjectByName(player.username);
        object.rotation.set(player.rx, player.ry, player.rz);
        object.position.set(player.x, player.y, player.z);
        let otherPlayer = this.getPlayerByName(player.username);
        Object.assign(otherPlayer, player);
      })


      // 监听用户拿起汉诺塔
      socket.on('OnPlayerPickUp', (res) => {
        console.log("onPlayerPickUo")
        console.log(res)
        console.log(this.columns[0].plates);
        console.log(this.columns[res.columnIndex].plates.length);

        let username = res.username;
        let index = res.index;
        this.isPicked = true; // 表明已经有人拿起了一块汉诺塔
        this.pickedUpPlateObject = scene.getObjectByName("plate" + index);
        // 更新房间信息
        this.originalColumn = res.columnIndex;
        //更新该玩家信息，表明该玩家持有该汉诺塔
        let player = this.getPlayerByName(username);


        console.log(this.columns[res.columnIndex].plates)
        console.log(this.columns[res.columnIndex].plates.length);
        this.columns[res.columnIndex].plates.pop();
        console.log("----------------")
        console.log(res.plate)
        player.plate = res.plate;
        console.log(player.plate)
        console.log(player)
        console.log("----------------------")
        console.log(this.columns[res.columnIndex].plates)
        console.log(this.columns[res.columnIndex].plates.length);

        console.log("onplayerpickup player:")
        console.log(player)
        // 更新柱子，使得该编号的汉诺塔从柱子上消失
        this.pickedUpPlateObject.visible = false;
      })
      // 监听用户放下汉诺塔
      socket.on('OnPlayerPutDown', (res) => {
        console.log("onPlayerPutDown")
        console.log(res)
        console.log(this.columns[this.originalColumn].plates.length)
        let username = res.username;
        let columnIndex = res.columnIndex;
        // 更新玩家信息
        let player = this.getPlayerByName(username);
        console.log("player:")
        console.log(player)
        console.log(player.plate)

        let plate = res.plate;

        console.log("nan:")
        console.log(this.columns[columnIndex].plates)
        console.log(plate)
        // 更新柱子上汉诺塔放下的情况
        let column = scene.getObjectByName('column' + columnIndex);
        this.pickedUpPlateObject.position.set(...column.position);  // 更改该块的位置
        this.pickedUpPlateObject.position.y = this.columns[columnIndex].plates.length * 10;  // 更新块的高度
        this.columns[columnIndex].plates.push(plate);
        this.pickedUpPlateObject.visible = true;
        //this.pickedUpPlateObject = null;
        this.isPicked = false;
        this.originalColumn = -1;
        console.log("putdown -----")
        console.log(this.pickedUpPlateObject)
        console.log(this.columns[columnIndex].plates)
        console.log(this.columns[columnIndex].plates.length)
        player.plate = null; // 更新该玩家信息
      })
    },
    updatePositionAndRotation() {
      if (this.role !== undefined) {
        this.player.x = this.role.position.x
        this.player.y = this.role.position.y
        this.player.z = this.role.position.z;
        this.player.ry = this.role.rotation.y;
      }
    },
    join() {
      console.log("join()")
      console.log(this.columns)
      //console.log(socket.connected)
      //console.log(this.player)
      socket.emit('OnJoin', this.player)
    },

    // 拿起汉诺塔事件
    pickUp() {
      console.log("pickUp()")
      console.log(this.columns)

      if (this.player.plate == null) {  // 该玩家还未拿起块
        // 玩家的位置
        console.log("player:")
        console.log(this.player)
        let playerPosition = new THREE.Vector3(this.player.x, this.player.y, 0);
        let index = -1; // 要拿起的汉诺塔的编号
        let columnIndex;
        for (let i = 0; i < this.columns.length; i++) {
          let column = scene.getObjectByName('column' + i);  // 获得柱子的模型
          let columnPosition = new THREE.Vector3(column.position.x, column.position.y, 0);
          if (playerPosition.distanceTo(columnPosition) < 25 &&
                  this.columns[i].plates.length > 0) { // 角色距离该柱子足够近且该柱子上存在汉诺塔
            let plates = this.columns[i].plates;
            index = plates[plates.length - 1].index;
            columnIndex = i;
            this.isPicked = true;
            break;
          }
        }
        // 编号大于0，表示拿到了汉诺塔
        if (index >= 0) {
          this.isPicked = true;
          let plates = this.columns[columnIndex].plates;
          console.log(plates.length)
          this.player.plate = plates.pop();
          // console.log("------------------------pickUp()--pop()前")
          // console.log(plates)
          //
          // console.log(plates.length)
          // console.log(this.columns[columnIndex].plates)
          // console.log(this.columns[columnIndex].plates.length)
          // console.log(this.player.plate)

          this.originalColumn = columnIndex;
          this.pickedUpPlateObject = scene.getObjectByName(('plate'+index));

          this.pickedUpPlateObject.visible = false; // 隐藏该模型

          socket.emit('OnPickUp', {username: this.player.username, columnIndex: columnIndex, index: index});
        }
      }
    },
    // 放下汉诺塔事件
    putDown() {
      console.log("puDown()")
      console.log(this.columns[0].plates)
      console.log(this.columns[0].plates.length)

      if (this.player.plate != null) { // 该角色有一个汉诺塔
        let rolePosition = new THREE.Vector3(this.player.x, this.player.y, 0);
        console.log("enter putdown:")
        let index = this.player.plate.index; // 放下的汉诺塔的编号
        let columnIndex = this.originalColumn;
        for (let i = 0; i < this.columns.length; i++) {
          let column = scene.getObjectByName('column' + i);  // 获得柱子的对象
          let columnPosition = new THREE.Vector3(column.position.x, column.position.y, 0);
          if (rolePosition.distanceTo(columnPosition) < 25) { // 角色距离该柱子足够近
            let plates = this.columns[i].plates;
            if (plates.length == 0 || plates[plates.length - 1].radius > this.player.plate.radius) {
              columnIndex = i;
            }
            break;
          }
        }
        socket.emit('OnPutDown', {username: this.player.username, columnIndex: columnIndex, index: index});

        console.log("username: "+this.player.username+ "columnIndex: "+columnIndex+"index: "+index)
        // 更新玩家信息
        let plate = this.player.plate;
        // console.log("player.plate:")
        // console.log(plate)
        // console.log(this.pickedUpPlateObject)
        this.player.plate = null; // 更新该玩家信息
        // 更新柱子上汉诺塔放下的情况
        let column = scene.getObjectByName('column' + columnIndex);
        this.pickedUpPlateObject.position.set(...column.position);  // 更改该块的位置
        this.pickedUpPlateObject.position.y = this.columns[columnIndex].plates.length * plate.height;  // 更新块的高度
        this.columns[columnIndex].plates.push(plate);
        this.pickedUpPlateObject.visible = true;
        this.originalColumn = -1;
        this.isPicked = false;
        console.log("putdown----后")
        console.log(this.columns[0].plates)
        console.log(this.columns[0].plates.length)
      }
    },
    deepClone(target) {
      // 定义一个变量
      let result;
      // 如果当前需要深拷贝的是一个对象的话
      if (typeof target === 'object') {
        // 如果是一个数组的话
        if (Array.isArray(target)) {
          result = []; // 将result赋值为一个数组，并且执行遍历
          for (let i in target) {
            // 递归克隆数组中的每一项
            result.push(this.deepClone(target[i]))
          }
          // 判断如果当前的值是null的话；直接赋值为null
        } else if(target===null) {
          result = null;
          // 判断如果当前的值是一个RegExp对象的话，直接赋值
        } else if(target.constructor===RegExp){
          result = target;
        }else {
          // 否则是普通对象，直接for in循环，递归赋值对象的所有值
          result = {};
          for (let i in target) {
            result[i] = this.deepClone(target[i]);
          }
        }
        // 如果不是对象的话，就是基本数据类型，那么直接赋值
      } else {
        result = target;
      }
      // 返回最终结果
      return result;
    }

  },
  mounted() {
    FirstPersonControls.bus = this.bus;
    this.init()
    this.createRole()
    this.getRoomInfo();
    this.socketInit();
    window.addEventListener('keydown', this.keyPressed, false);
    window.addEventListener('keyup', this.keyUp, false)
    this.bus.on('modifyRole', () => {
      this.updatePositionAndRotation()
      socket.emit('OnUpdate', this.player)
    })
    this.animate()
  }


}
</script>

<style scoped>

</style>
