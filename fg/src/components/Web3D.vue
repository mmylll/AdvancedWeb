<template>
  <div id="main">

  </div>
</template>

<script>
import * as THREE from 'three'
import FirstPersonControls from "../../public/js/FirstPersonControls";
import {OrbitControls} from 'three/examples/jsm/controls/OrbitControls.js'
import {ConvexGeometry} from 'three/examples/jsm/geometries/ConvexGeometry.js'
import {BufferGeometryUtils} from 'three/examples/jsm/utils/BufferGeometryUtils.js'
import {GLTFLoader} from 'three/examples/jsm/loaders/GLTFLoader'
import {DRACOLoader} from 'three/examples/jsm/loaders/DRACOLoader'
import {FBXLoader} from 'three/examples/jsm/loaders/FBXLoader'
import {AnimationMixer} from "three";

let scene, role;
export default {
  name: "Web3D",
  data() {
    return {
      camera: null,
      render: null,
      back: require('../assets/skyBox/back.jpg'),
      bottom: require('../assets/skyBox/bottom.jpg'),
      left: require('../assets/skyBox/left.jpg'),
      right: require('../assets/skyBox/right.jpg'),
      top: require('../assets/skyBox/top.jpg'),
      front: require('../assets/skyBox/front.jpg'),
      texture: {
        floor: require('../assets/texture/floor.jpg')
      },

      // 角色
      stateList: {},
      actionMap: {
        up: {direction: 'up', rotation: Math.PI, axes: 'z'},
        down: {direction: 'down', rotation: 0, axes: 'z'},
        left: {direction: 'left', rotation: -Math.PI / 2, axes: 'x'},
        right: {direction: 'right', rotation: Math.PI / 2, axes: 'x'}
      },
      nopeAction: {direction: null},
      nextAction: {direction: 'down', rotation: 0},
      clock: null,
      mixer: null,
      currentAction: null,
      previousAction: null,
      lastkey: '',
      firstPersonControl: null
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
      //this.addMeshes()
      this.addLight()
      // this.addCylinder(3)
      document.getElementById('main').appendChild(this.render.domElement)
      // let controls = new OrbitControls(this.camera, this.render.domElement)
      // controls.update()
      this.renderCanvas()
    },
    addMeshes() {
      let plane = new THREE.PlaneGeometry(800, 800)
      let floorTexture = new THREE.TextureLoader().load(this.texture.floor, () => {
        floorTexture.wrapS = THREE.RepeatWrapping
        floorTexture.wrapT = THREE.RepeatWrapping
      })
      let planeMaterial = new THREE.MeshLambertMaterial({
        map: floorTexture,
        side: THREE.DoubleSide
      })
      let planeMesh = new THREE.Mesh(plane, planeMaterial)
      scene.add(planeMesh)
      this.camera.lookAt(planeMesh)
      planeMesh.name = 'planeMesh'
      planeMesh.rotation.x = Math.PI / 2
      planeMesh.position.set(0, -1, 0)
    },

    addCylinder(number) {
      let geometry, cylinder
      let material = new THREE.MeshBasicMaterial({color: 0xffff00});
      for (let i = 0; i < 3; i++) {
        geometry = new THREE.CylinderGeometry(5, 5, 150, 32);
        cylinder = new THREE.Mesh(geometry, material);
        scene.add(cylinder)
        cylinder.name = 'cylinder' + i
        cylinder.position.set(-200 + 200 * i, 75, -50)
      }
      cylinder = scene.getObjectByName('cylinder0')
      let littleCylinder
      material = new THREE.MeshLambertMaterial({color: 0xffff00})
      for (let i = 0; i < number; i++) {
        geometry = new THREE.CylinderGeometry(20 * (number - i), 20 * (number - i), 10, 32)
        littleCylinder = new THREE.Mesh(geometry, material)
        scene.add(littleCylinder)
        littleCylinder.name = 'littleCylinder' + i
        littleCylinder.position.set(cylinder.position.x, 5 + 10 * i, cylinder.position.z)
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
          map:
              textureLoader.load(this.right), side: THREE.BackSide
        }),
// right
        new THREE.MeshBasicMaterial({
          map:
              textureLoader.load(this.left), side: THREE.BackSide
        }),
// left
        new THREE.MeshBasicMaterial({
          map:
              textureLoader.load(this.top), side: THREE.BackSide
        }),
// top
        new THREE.MeshBasicMaterial({
          map:
              textureLoader.load(this.bottom), side: THREE.BackSide
        }),
// bottom
        new THREE.MeshBasicMaterial({
          map:
              textureLoader.load(this.front), side: THREE.BackSide
        }),
// back
        new THREE.MeshBasicMaterial({
          map:
              textureLoader.load(this.back), side: THREE.BackSide
        })
// front
      ];
// 创建天空盒子并添加到场景
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
      //scene.background = new THREE.CubeTextureLoader().load([this.right, this.left, this.top, this.bottom, this.front, this.back])
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
      //this.handleRoleAction()
      this.renderCanvas()
      requestAnimationFrame(() => {
        this.animate()
      })
    },


    // 创建人物
    createRole() {

      // const loader = new FBXLoader();
      // loader.load('3D/Idle.fbx', (object) => {
      //   this.role = object;
      //   this.mixer = new AnimationMixer(object);
      //   object.scale.set(0.1, 0.1, 0.1)
      //   console.log(object.animations)
      //   this.stateList.Walking = this.mixer.clipAction(object.animations[1])
      //   this.currentAction = this.stateList.Walking;
      //   this.currentAction.play()
      //   object.traverse(function (child) {
      //     if (child.isMesh) {
      //       child.castShadow = true;
      //       child.receiveShadow = true;
      //     }
      //   });
      //   scene.add(object)
      // })
      // model
      const loader = new GLTFLoader()
      const dracoLoader = new DRACOLoader()
      dracoLoader.setDecoderPath('/draco/')
      dracoLoader.preload()
      loader.setDRACOLoader(dracoLoader)

      loader.load('3D/RobotExpressive.glb', gltf => {
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
      }, undefined, function (e) {
        console.error(e);
      });
    },
    keyPressed(event) {
      if (this.lastkey !== event.keyCode) {
        this.lastkey = event.keyCode;
        this.fadeToAction('Walking', 0.2);
      }
      //this.fadeToAction('Walking', 0.2);
      // switch (key) {
      //   case 87:
      //     /*w*/
      //     this.nextAction = this.actionMap.up;
      //     break;
      //   case 65:
      //     /*a*/
      //     this.nextAction = this.actionMap.left;
      //     break;
      //
      //   case 83:
      //     /*s*/
      //     this.nextAction = this.actionMap.down;
      //     break;
      //   case 68:
      //     /*d*/
      //     this.nextAction = this.actionMap.right;
      //     break;
      // }
      //if (this.role) this.role.rotation.y = this.nextAction.rotation;
    },
    keyUp() {
      this.lastkey = null;
      this.nextAction = this.nopeAction;
      this.fadeToAction('Standing', 0.2);
    },
    // handleRoleAction() {
    //   var flag = 0
    //   if (this.role) {
    //     if (this.nextAction.direction == 'down' || this.nextAction.direction == "right") {
    //       flag = 1;
    //     } else if (this.nextAction.direction == 'up' || this.nextAction.direction == "left") {
    //       flag = -1;
    //     } else {
    //       flag = 0;
    //     }
    //     this.role.position[this.nextAction.axes] += flag;
    //   }
    // },
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
    }
  },
  mounted() {
    this.init()
    this.createRole()
    window.addEventListener('keydown', this.keyPressed, false);
    window.addEventListener('keyup', this.keyUp, false)
    this.animate()
  }


}
</script>

<style scoped>

</style>
