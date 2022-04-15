<template>
  <div id="main">

  </div>
</template>

<script>
import * as THREE from 'three'
import {OrbitControls} from 'three/examples/jsm/controls/OrbitControls.js'
import {ConvexGeometry} from 'three/examples/jsm/geometries/ConvexGeometry.js'
import {BufferGeometryUtils} from 'three/examples/jsm/utils/BufferGeometryUtils.js'

let scene
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
      background: require('../assets/skyBox/back3.jpg'),
      texture: {
        floor: require('../assets/texture/floor.jpg')
      }
    }
  },
  methods: {
    init() {
      scene = new THREE.Scene()
      this.render = new THREE.WebGLRenderer({antialias: true})
      this.camera = new THREE.PerspectiveCamera(75, window.innerWidth / window.innerHeight, 0.1, 1000)
      this.camera.position.set(0, 5, 10)
      this.camera.lookAt(0, 10, 0)

      this.render.setClearColor(0xffffff, 1.0)
      this.render.setSize(window.innerWidth, window.innerHeight)

      this.setBackground()
      this.addMeshes()
      this.addLight()
      document.getElementById('main').appendChild(this.render.domElement)
      let controls = new OrbitControls(this.camera, this.render.domElement)
      controls.update()
      this.renderCanvas()
    },
    addMeshes() {
      let plane = new THREE.PlaneGeometry(300, 300)
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
      planeMesh.name = 'planeMesh'
      planeMesh.rotation.x = Math.PI / 2
      planeMesh.position.set(0, -1, 0)
    },
    addLight() {
      let light = new THREE.AmbientLight('#ECE20E',1)
      scene.add(light)
    },

    setBackground() {
      scene.background = new THREE.CubeTextureLoader().load([this.right, this.left, this.top, this.bottom, this.front, this.back])
      // let loader = new THREE.TextureLoader()
      // let texture = loader.load(this.background, () => {
      //   let rt = new THREE.WebGLRenderTargetCube(texture.image.width,texture.image.height)
      //   rt.fromEquirectangularTexture(this.render, texture)
      //   scene.background = rt.texture
      // })
    },
    renderCanvas() {
      this.render.render(scene, this.camera)
    },
    animate() {
      this.renderCanvas()
      requestAnimationFrame(() => {
        this.animate()
      })
    }
  },
  mounted() {
    this.init()
    this.animate()

  }
}
</script>

<style scoped>

</style>