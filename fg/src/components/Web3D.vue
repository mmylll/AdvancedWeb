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
  import {getCurrentInstance} from "vue";
  import socket from "@/socket";

  let scene, role
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
          ry: Math.PI,
          plate: null
        },
        stateList: {},
        clock: null,
        mixer: null,
        currentAction: null,
        previousAction: null,

        lastkey: '',
        firstPersonControl: null,

        // 储存其它玩家，不包括自己
        otherPlayer: [],
        columns: [],
        loader: null,
        dracoLoader: null,
        animations: {
          walking: null,
          standing: null
        },
        // 总线
        bus: getCurrentInstance().appContext.config.globalProperties.$bus,
        // 被拿起块的模型对象
        pickedUpPlateObject: null,
        isPicked: false,
        // 被拿起的块的原来的柱子编号
        originColumn: -1,
        copyColumns: [],
        colors: ["#ff0000", "#00ff00", "#0000ff"],
        inputing: false,
        animationKey: null
      }
    },
    methods: {
      // 初始化相机和第一人称的的视角
      init() {
        scene = new THREE.Scene()
        this.render = new THREE.WebGLRenderer({antialias: true})
        this.camera = new THREE.PerspectiveCamera(75, window.innerWidth / window.innerHeight, 0.1, 1000)
        this.camera.position.set(0, 100, 0)
        this.camera.rotation.set(0, Math.PI, 0);

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
      // 退出3D窗口
      quit() {
        if (this.player.plate !== null) {
          this.$message({
            message: '必须放下持有的圆盘，才能离开场景',
            type: 'error'
          })
        } else {
          // 断开socket连接
          socket.disconnect();
          socket.close();
          // 移除场景中的当前玩家
          scene.remove(role)
          this.firstPersonControl.role = null
          // 关闭第一人称视角注册的事件
          this.firstPersonControl.disconnect()
          scene.remove(this.firstPersonControl.yawObject)
          this.firstPersonControl = null;
          FirstPersonControls.bus = null
          role = null;
          // 取消动画
          cancelAnimationFrame(this.animationKey)
          this.quitDialog = true;
          this.$router.replace('/About')
        }
      },
      // 创建汉诺塔的三根柱子与上面的块
      addCylinder() {
        let geometry, cylinder
        let material = new THREE.MeshBasicMaterial({color: 0xC4C2C0})
        for (let i = 0; i < this.columns.length; i++) {
          let column = this.columns[i];
          geometry = new THREE.CylinderGeometry(5, 5, 100.0, 32);
          cylinder = new THREE.Mesh(geometry, material);
          scene.add(cylinder)
          cylinder.name = 'column' + i
          cylinder.position.set(100 * (i - 1), 0, -50)
          // 块的初始高度为3.0
          let height = 3.0;
          for (let plate of column.plates) {
            let plateGeometry = new THREE.CylinderGeometry(plate.radius, plate.radius, plate.height, 32);
            // 块的颜色为colors的3个颜色量中的一种
            let plateMaterial = new THREE.MeshLambertMaterial({color: this.colors[plate.index % 3]})
            let plateMesh = new THREE.Mesh(plateGeometry, plateMaterial);
            scene.add(plateMesh)
            plateMesh.name = 'plate' + plate.index;
            plateMesh.position.set(...cylinder.position);
            plateMesh.position.y = height;
            // 设置块的高度叠加
            height += plate.height;
          }
        }
      },
      // 增加环境光
      addLight() {
        let light = new THREE.AmbientLight(' 0xaaaaaa', 1)
        scene.add(light)
      },
      // 设置背景纹理贴图
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
      // 渲染场景
      renderCanvas() {
        this.render.render(scene, this.camera)
      },
      // 实现动画效果
      animate() {
        let dt = this.clock.getDelta()
        if (this.mixer) {
          this.mixer.update(dt)
        }

        this.firstPersonControl.update(dt);
        this.renderCanvas()
        this.animationKey = requestAnimationFrame(() => {
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
          role = gltf.scene
          role.position.y = 0
          role.scale.set(7, 7, 7)// 设置模型大小
          role.rotation.y = Math.PI
          this.mixer = new THREE.AnimationMixer(role);
          this.animations.walking = gltf.animations[10]
          this.animations.standing = gltf.animations[8]
          this.stateList.Walking = this.mixer.clipAction(gltf.animations[10]);
          this.stateList.Standing = this.mixer.clipAction(gltf.animations[8]);
          // 设置下面两项主要是站着的时候
          this.stateList.Standing.clampWhenFinished = true;
          this.stateList.Standing.loop = THREE.LoopOnce;
          this.currentAction = this.stateList.Standing;
          this.currentAction.play();
          scene.add(role);
          this.firstPersonControl.role = role
          this.updatePositionAndRotation()
        }, undefined, function (e) {
          console.error(e);
        });
      },
      // 监听键盘按下事件
      keyPressed(event) {
        // 按下q键，打开退出的弹出框
        if (event.keyCode === 81) {
          this.quitDialog = true;
        } else if (event.keyCode === 32) { // 当按下空格
          if (!this.isPicked) { // 还没有人拿汉诺塔
            this.pickUp();
          }
          // 按下w、s、a、d移动键
        } else if (event.keyCode === 87 || event.keyCode === 83 || event.keyCode === 65 || event.keyCode === 68 || this.lastkey !== event.keyCode) {
          this.lastkey = event.keyCode;
          this.fadeToAction('Walking', 0.2);
        }
      },
      // 按键松开
      keyUp(event) {
        // 松开空格
        if (event.keyCode === 32) {
          // 判断当前玩家是否拿起块，是就调用putDown放下块
          if (this.isPicked && this.player.plate !== null) {
            this.putDown();
          }
        } else if (event.keyCode !== 81) {
          this.lastkey = null;
          this.fadeToAction('Standing', 0.2);
        }
      },
      // 实现行走站立动画
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
      // 渲染其它的玩家
      renderOtherPlayer() {
        for (let player of this.otherPlayer) {
          this.renderAPlayer(player);
        }
      },
      // 加载玩家的模型并假如到场景中
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
      // 获得房间初始信息
      // 房间初始信息包括三根柱子、每根柱子上的块层数、其它所有玩家
      getRoomInfo() {
        this.axios.get('/Join').then((res) => {
          this.columns = res.data.data.columns;
          for (let i in res.data.data.players) {
            this.otherPlayer.push(res.data.data.players[i]);
          }
          this.addCylinder();
          this.renderOtherPlayer();
        }).then(() => {
          this.socketInit()
        }).then(() => {
          this.join()
        })
      },
      // 通过用户名获取房间中玩家对象
      getPlayerByName(username) {
        for (let player of this.otherPlayer) {
          if (player.username === username)
            return player;
        }
        return null
      },
      // 初始化socketio
      socketInit() {
        // 向后端连接socket
        socket.connect();
        // 监听连接事件
        socket.on('connect', () => {
        })
        // 监听断开连接事件
        socket.on('disconnect', () => {
          this.$message({
            message: "退出成功",
            type: 'success'
          })
        })
        // 监听玩家加入
        socket.on('OnPlayerJoin', (player) => {
          let object = scene.getObjectByName(player.player.username)
          if (object) {
            object.removeFromParent();
          }
          this.otherPlayer.push(player.player)
          console.log(scene.children)
          this.renderAPlayer(player.player);
          console.log(scene.children)
          this.$message({
            message: player.player.username + "加入了游戏",
            type: 'success'
          })
        })
        // 监听玩家离开
        socket.on('OnPlayerLeave', (res) => {
          let username = res.username;
          let player = this.getPlayerByName(username);
          if (player !== null) {
            this.otherPlayer.splice(this.otherPlayer.indexOf(player), 1);
            let object = scene.getObjectByName(username);
            object.removeFromParent()
            this.$message({
              message: username + "离开了游戏",
              type: 'success'
            })
          }
        })
        // 监听玩家的位置和转向发生变化
        socket.on('OnPlayerUpdate', (res) => {
          let player = res.player;
          let object = scene.getObjectByName(player.username);
          if (object) {
            object.rotation.set(player.rx, player.ry, player.rz);
            object.position.set(player.x, player.y, player.z);
            let otherPlayer = this.getPlayerByName(player.username);
            Object.assign(otherPlayer, player);
          }
        })


        // 监听用户拿起汉诺塔
        socket.on('OnPlayerPickUp', (res) => {
          let username = res.username;
          let index = res.index;
          this.isPicked = true; // 表明已经有人拿起了一块汉诺塔
          this.pickedUpPlateObject = scene.getObjectByName("plate" + index);
          // 更新房间信息
          this.originalColumn = res.columnIndex;
          //更新该玩家信息，表明该玩家持有该汉诺塔
          let player = this.getPlayerByName(username);
          this.columns[res.columnIndex].plates.pop();

          player.plate = res.plate;
          // 更新柱子，使得该编号的汉诺塔从柱子上消失
          this.pickedUpPlateObject.visible = false;
        })
        // 监听用户放下汉诺塔
        socket.on('OnPlayerPutDown', (res) => {
          let username = res.username;
          let columnIndex = res.columnIndex;
          // 更新玩家信息
          let player = this.getPlayerByName(username);
          let plate = res.plate;

          // 更新柱子上汉诺塔放下的情况
          let column = scene.getObjectByName('column' + columnIndex);
          this.pickedUpPlateObject.position.set(...column.position);  // 更改该块的位置
          this.pickedUpPlateObject.position.y = this.columns[columnIndex].plates.length * plate.height + 3.0;  // 更新块的高度
          this.columns[columnIndex].plates.push(plate);
          this.pickedUpPlateObject.visible = true;
          this.isPicked = false;
          this.originalColumn = -1;
          player.plate = null; // 更新该玩家信息
        })
        // 监听是否有其它玩家正在拿起，防止两个玩家同时拿起块
        socket.on('PickedUp', (res) => {
          let state = res.state;
          // 没有其它玩家正在拿
          if (state) {
            this.isPicked = true;
            let plates = this.columns[this.originalColumn].plates;
            this.player.plate = plates.pop();
            this.pickedUpPlateObject = scene.getObjectByName(('plate' + this.index));
            this.pickedUpPlateObject.visible = false; // 隐藏该模型
          } else {
            this.$message({
              message: '拿起失败，上层圆盘拿起未被放下',
              type: 'warning'
            })
          }
        })
      },
      // 更新当前玩家的位置和转向
      updatePositionAndRotation() {
        if (role !== undefined) {
          this.player.x = role.position.x
          this.player.y = role.position.y
          this.player.z = role.position.z;
          this.player.ry = role.rotation.y;
        }
      },
      // 发送当前加入到房间的事件
      join() {
        socket.emit('OnJoin', this.player)
      },

      // 拿起汉诺塔事件
      pickUp() {
        if (this.player.plate == null) {  // 该玩家还未拿起块
          // 玩家的位置
          let playerPosition = new THREE.Vector3(this.player.x, 0, this.player.z);
          let index = -1; // 要拿起的汉诺塔的编号
          let columnIndex;
          for (let i = 0; i < this.columns.length; i++) {
            let column = scene.getObjectByName('column' + i);  // 获得柱子的模型
            let columnPosition = new THREE.Vector3(column.position.x, column.position.y, column.position.z);
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
            socket.emit('OnPickUp', {username: this.player.username, columnIndex: columnIndex, index: index});
            this.originalColumn = columnIndex;
            this.index = index;
          } else {
            this.$message({
              message: '不在拿起距离内',
              type: 'warning'
            })
          }
        }
      },
      // 放下汉诺塔事件
      putDown() {
        if (this.player.plate != null) { // 该角色有一个汉诺塔
          let rolePosition = new THREE.Vector3(this.player.x, this.player.y, 0);
          let index = this.player.plate.index; // 放下的汉诺塔的编号
          let columnIndex = this.originalColumn;
          for (let i = 0; i < this.columns.length; i++) {
            let column = scene.getObjectByName('column' + i);  // 获得柱子的对象
            let columnPosition = new THREE.Vector3(column.position.x, column.position.y, 0);
            if (rolePosition.distanceTo(columnPosition) < 25) { // 角色距离该柱子足够近
              let plates = this.columns[i].plates;
              if (plates.length === 0 || plates[plates.length - 1].radius > this.player.plate.radius) {
                columnIndex = i;
              } else {
                this.$message({
                  message: '放置不符合规制',
                  type: 'warning'
                })
              }
              break;
            }
          }
          socket.emit('OnPutDown', {username: this.player.username, columnIndex: columnIndex, index: index});

          // 更新玩家信息
          let plate = this.player.plate;
          this.player.plate = null; // 更新该玩家信息
          // 更新柱子上汉诺塔放下的情况
          let column = scene.getObjectByName('column' + columnIndex);
          this.pickedUpPlateObject.position.set(...column.position);  // 更改该块的位置
          this.pickedUpPlateObject.position.y = this.columns[columnIndex].plates.length * plate.height + 3.0;  // 更新块的高度
          this.columns[columnIndex].plates.push(plate);
          this.pickedUpPlateObject.visible = true;
          this.originalColumn = -1;
          this.isPicked = false;
        }
      }

    }
    ,
    mounted() {
      FirstPersonControls.bus = this.bus;
      this.init()
      this.createRole()
      this.getRoomInfo();
      window.addEventListener('keydown', this.keyPressed, false);
      window.addEventListener('keyup', this.keyUp, false)
      // 监听从第一人称控制的对象移动时发送的事件
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
