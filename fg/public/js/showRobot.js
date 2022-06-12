import * as THREE from "three";
import { OrbitControls } from "three/examples/jsm/controls/OrbitControls"; // 轨道控制器
import { GLTFLoader } from "three/examples/jsm/loaders/GLTFLoader"; // 文件格式加载器

class Base3d {
    constructor(selector) {
        this.container = document.querySelector(selector)
        this.camera; // 摄像机
        this.scene; // 场景
        this.renderer; // 渲染器
        this.init(); // 初始化
        this.animate(); // 动画不断渲染
    }

    // 初始化
    init() {
        this.initScene(); // 场景
        this.initCamera(); // 摄像机
        this.initRenderer(); // 渲染器
        this.initControls(); // 轨道控制器(放在渲染器后面)
        this.addMesh(); // 添加物体
        window.addEventListener('resize', this.onWindowReset.bind(this))
    }

    // 初始化场景
    initScene() {
        this.scene = new THREE.Scene({antialias: true});
    }

    // 初始化摄像机
    initCamera() {
        // 透视相机
        this.camera = new THREE.PerspectiveCamera(
            60,
            window.innerWidth / window.innerHeight,
            4,
            100
        );
        // 相机3维坐标
        this.camera.position.set(2, 1.0, 5);
    }

    // 初始化渲染器
    initRenderer() {
        // 渲染器-抗锯齿
        this.renderer = new THREE.WebGLRenderer({ antialias: true });

        // 设置像素比(定义为浏览器的缩放比)
        this.renderer.setPixelRatio(window.devicePixelRatio);

        // 设置渲染器尺寸大小
        this.renderer.setSize(this.container.clientWidth, this.container.clientHeight);
        this.renderer.setClearColor(0xb9d3ff, 1);
        // 绘制输出一个 canvas
        this.container.appendChild(this.renderer.domElement);
    }

    // 渲染函数
    render() {
        // 用相机渲染一个场景
        this.renderer.render(this.scene, this.camera);
    }

    // 动画
    animate() {
        // 每一帧都会调用此方法
        this.renderer.setAnimationLoop(this.render.bind(this))
    }

    // 轨道控制器
    initControls() {
        // this.camera是个object（必须）将要被控制的相机
        this.controls = new OrbitControls(this.camera, this.renderer.domElement)
    }

    // 设置模型（物体）
    setModel(modelName) {
        return new Promise(() => {
            const loader = new GLTFLoader().setPath('../3D/');
            loader.load(modelName, (gltf) => {
                gltf.scene.traverse( function ( child ) {
                    if ( child.isMesh ) {
                        child.frustumCulled = false;
                        //模型阴影
                        child.castShadow = true;
                        //模型自发光
                        child.material.emissive =  child.material.color;
                        child.material.emissiveMap = child.material.map ;
                    }})
                const model = gltf.scene; // 取屏幕的第一个子元素
                model.position.y = -2
                model.scale.set(0.85, 0.85, 0.85)// 设置模型大小
                this.scene.add(model); // 给屏幕添加模型（即物体）
            })
        })
    }

    // 添加物体
    addMesh() {
        this.setModel('RobotExpressive.glb')
    }

    // 监听浏览器，做自适应渲染
    onWindowReset() {
        // 摄像机视椎体的长宽比
        this.camera.aspect = window.innerWidth / window.innerHeight;
        // 更新摄像机投影矩阵
        this.camera.updateProjectionMatrix();
        // 渲染器重新设置大小
        this.renderer.setSize(this.container.clientWidth, this.container.clientHeight);
    }
}

export default Base3d
