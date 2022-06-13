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

      pickedUpPlate: null, // 被拿起的块的对象
      pickedUpPlateObject: null, // 被拿起块的模型对象
      isPicked: false,
      originColumn: -1 // 被拿起的块的原来的柱子编号
    }
  },
  methods: {
    init() {
      scene = new THREE.Scene()
      this.render = new THREE.WebGLRenderer({antialias: true})
      this.camera = new THREE.PerspectiveCamera(75, window.innerWidth / window.innerHeight, 0.1, 1000)
      this.camera.position.set(0, 100, 0)

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
          height += 10.0;
          let plateGeometry = new THREE.CylinderGeometry(plate.radius, plate.radius, height, 32);
          let plateMesh = new THREE.Mesh(plateGeometry, plateMaterial)
          scene.add(plateMesh)
          plateMesh.name = 'plate' + plate.index;
          plateMesh.position.set(...cylinder.position);
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
        if (this.isPicked && this.player.plate > 0) {
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
      this.axios.get('/Join').then((res) => {
        console.log(res)
        this.columns = res.data.data.columns;
        console.log('other players:')
        for (let i in res.data.data.players) {
          console.log(i)
          this.otherPlayer.push(res.data.data.players[i]);
          console.log(res.data.data.players[i])
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
        this.otherPlayer.push(player.player)
        console.log(player)
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
      socket.on('OnPlayerPickUp', (username, index) => {
        this.isPicked = true; // 表明已经有人拿起了一块汉诺塔
        this.pickedUpPlateObject = scene.getObjectByName("plate" + index);
        // 更新房间信息

        for (let i = 0; i < this.columns.length; i++) {
          let plates = this.columns[i].plates;
          if (plates.length > 0 && plates[plates.length - 1].index == index) {
            this.pickedUpPlate = this.columns[i].plates.pop();
            this.originalColumn = i;
          }
        }
        //更新该玩家信息，表明该玩家持有该汉诺塔
        let player = this.getPlayerByName(username);
        player.plate = this.pickedUpPlate;
        // 更新柱子，使得该编号的汉诺塔从柱子上消失
        scene.remove(this.pickedUpPlateObject);
      })
      // 监听用户放下汉诺塔
      socket.on('OnPlayerPutDown', (username, columnIndex, index) => {
        // 更新玩家信息
        let player = this.getPlayerByName(username);
        player.plate = null; // 更新该玩家信息
        // 更新柱子上汉诺塔放下的情况
        let column = scene.getObjectByName('column' + columnIndex);
        let putPlateHeight = this.columns[columnIndex].plates.length * this.pickedUpPlate.height;
        this.columns[columnIndex].plates.push(this.pickedUpPlate);
        this.pickedUpPlateObject.position.set(column.position);  // 更改该块的位置
        this.pickedUpPlateObject.position.y += putPlateHeight;  // 更新块的高度
        scene.add(this.pickedUpPlateObject);
        this.pickedUpPlateObject = null;
        this.pickedUpPlate = null;
        this.isPicked = false;
        this.originalColumn = -1;
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
      //console.log(socket.connected)
      console.log(this.player)
      socket.emit('OnJoin', this.player)
    },

    // 拿起汉诺塔事件
    pickUp() {
      if (this.player.plate == null) {  // 该玩家还未拿起块
        // 玩家的位置
        let playerPosition = new THREE.Vector3(this.player.position.x, this.player.position.y, 0);
        let index = -1; // 要拿起的汉诺塔的编号
        for (let i = 0; i < this.columns.length; i++) {
          let column = scene.getObjectByName('column' + i);  // 获得柱子的模型
          let columnPosition = new THREE.Vector3(column.position.x, column.position.y, 0);
          if (playerPosition.distanceTo(columnPosition) < 10 &&
              this.columns[i].plates.length > 0) { // 角色距离该柱子足够近且该柱子上存在汉诺塔
            let plates = this.columns[i].plates;
            index = plates[plates.length - 1].index;
            this.isPicked = true;
            break;
          }
        }
        // 编号大于0，表示拿到了汉诺塔
        if (index >= 0) {
          socket.emit('OnPickUp', {username: this.player.username, index: index});
        }
      }
    },
    // 放下汉诺塔事件
    putDown() {
      if (this.player.plate != null) { // 该角色有一个汉诺塔
        let rolePosition = new THREE.Vector3(this.role.position.x, this.role.position.y, 0);
        let index = this.player.plate.index; // 放下的汉诺塔的编号
        let isOk = false; // 判断是否能放下该块
        let columnIndex = -1;
        for (let i = 0; i < this.columns.length; i++) {
          let column = scene.getObjectByName('column' + i);  // 获得柱子的对象
          let columnPosition = new THREE.Vector3(column.position.x, column.position.y, 0);
          if (rolePosition.distanceTo(columnPosition) < 10) { // 角色距离该柱子足够近
            let plates = this.columns[i].plates;
            if (plates.length == 0) {
              isOk = true;
            } else if (plates[plates.length - 1].radius > this.role.plate.radius) {
              isOk = true;
            }
            columnIndex = i;
            break;
          }
        }
        if (isOk) {
          socket.emit('OnPutDown', {username: this.player.username, columnIndex: columnIndex, index: index});
        } else {  // 放回原位
          socket.emit('OnPutDown', {username: this.player.username, columnIndex: this.originalColumn, index: index});
        }
      }

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
