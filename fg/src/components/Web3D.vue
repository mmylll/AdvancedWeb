<template>
  <div id="main">
    <el-dialog
        v-model="quitDialog"
        title="退出"
        width="30%"
        center
    >
      <h2>是否退出</h2>
      <div>
        <el-button @click="quit">确定</el-button>
        <el-button @click="quitDialog=false">取消</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import * as THREE from 'three'
import FirstPersonControls from "../../public/js/FirstPersonControls";
import {GLTFLoader} from 'three/examples/jsm/loaders/GLTFLoader'
import {DRACOLoader} from 'three/examples/jsm/loaders/DRACOLoader'
import socket from "@/socket";
import {getCurrentInstance} from "vue";

let scene;
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
        rx: 0,
        rz: 0,
        ry: 0,
        plate: null
      },
      role: null,
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
      bus: getCurrentInstance().appContext.config.globalProperties.$bus
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
      if (!this.player.plate) {
        this.$message({
          message: '必须放下持有的圆盘，才能离开场景',
          type: 'error'
        })
      } else {
        socket.on('Leave', this.player).then(() => {
          socket.disconnect()
          this.$message({
            message: "退出成功",
            type: 'success'
          })
        })
      }
      this.quitDialog = false
    },

    addCylinder() {
      let geometry, cylinder
      let material = new THREE.MeshBasicMaterial({color: 0xC4C2C0}),
          plateMaterial = new THREE.MeshLambertMaterial({color: 0xffff00})
      for (let i = 0; i < this.columns.length; i++) {
        let column = this.columns[i];
        geometry = new THREE.CylinderGeometry(5, 5, column.height, 32);
        cylinder = new THREE.Mesh(geometry, material);
        scene.add(cylinder)
        cylinder.name = 'column' + i
        cylinder.position.set(-200 + 200 * i, 75, -50)
        for (let plate of column.plates) {
          let plateGeometry = new THREE.CylinderGeometry(plate.radius, plate.radius, plate.height, 32);
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
        this.stateList.Walking = this.mixer.clipAction(gltf.animations[10]);
        this.stateList.Standing = this.mixer.clipAction(gltf.animations[8]);
        // 设置下面两项主要是站着的时候，别抖了
        this.stateList.Standing.clampWhenFinished = true;
        this.stateList.Standing.loop = THREE.LoopOnce;
        this.currentAction = this.stateList.Standing;
        this.currentAction.play();
        scene.add(this.role);
        this.firstPersonControl.role = this.role
        this.player.username = this.$store.state.username;
        this.updatePositionAndRotation()
      }, undefined, function (e) {
        console.error(e);
      });
    },
    keyPressed(event) {
      if (event.keyCode === 81) {
        this.quitDialog = !this.quitDialog
      } else if (this.lastkey !== event.keyCode) {
        this.lastkey = event.keyCode;
        this.fadeToAction('Walking', 0.2);
      }
    },
    keyUp() {
      this.lastkey = null;
      this.nextAction = this.nopeAction;
      this.fadeToAction('Standing', 0.2);
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
        role.rotation.set(player.rx, player.ry, player.rz);
        role.position.set(player.x, player.y, player.z)
        scene.add(role);
        let mixer = new THREE.AnimationMixer(role);
        mixer.clipAction(this.animations.standing).play();
        role.name = player.username
      })
    },
    getRoomInfo() {
      this.axios.get('/RoomInfo').then((res) => {
        this.columns = res.data.columns;
        this.otherPlayer = res.data.players;
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
      if (!socket.connected) {
        socket.connect();
      }
      socket.on('PlayerJoin', (player) => {
        this.otherPlayer.push(player)
        this.renderAPlayer(player);
        this.$message({
          message: player.username + "加入了游戏",
          type: 'success'
        })
      })
      socket.on('PlayerLeave', (username) => {
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
      socket.on('PlayerUpdate', (player) => {
        let object = scene.getObjectByName(player.username);
        object.rotation.set(player.rx, player.ry, player.rz);
        object.position.set(player.x, player.y, player.z);
        let otherPlayer = this.getPlayerByName(player.username);
        Object.assign(otherPlayer, player);
      })
    },
    updatePositionAndRotation() {
      this.player.x = this.role.position.x
      this.player.y = this.role.position.y
      this.player.z = this.role.position.z;
      this.player.ry = this.role.rotation.y;
    },
    join() {
      socket.on('Join', this.player)
    }
  },
  mounted() {
    this.init()
    this.getRoomInfo();
    this.createRole()
    this.join();
    window.addEventListener('keydown', this.keyPressed, false);
    window.addEventListener('keyup', this.keyUp, false)
    this.bus.on('modifyRole', () => {
      this.updatePositionAndRotation()
      socket.emit('Update', this.player)
    })
    this.animate()
  }


}
</script>

<style scoped>

</style>
