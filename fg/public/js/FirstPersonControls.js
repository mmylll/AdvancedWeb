import * as THREE from 'three'

// W S A D 的keycode
const KEY_W = 87;
const KEY_S = 83;
const KEY_A = 65;
const KEY_D = 68;

class FirstPersonControls {
    static bus;

    constructor(camera, domElement) {
        this.domElement = domElement || document.body;
        this.isLocked = false;
        this.camera = camera;

        // 初始化camera, 将camera放在pitchObject正中央
        camera.rotation.set(0, 0, 0);
        camera.position.set(0, 0, 0);

        // 将camera添加到pitchObject, 使camera沿水平轴做旋转, 并提升pitchObject的相对高度
        this.pitchObject = new THREE.Object3D();
        this.pitchObject.add(camera);
        this.pitchObject.position.y = 15;
        this.pitchObject.position.z = -8;
        // 将pitObject添加到yawObject, 使camera沿竖直轴旋转
        this.yawObject = new THREE.Object3D();
        this.yawObject.add(this.pitchObject);

        // 初始化移动状态
        this.moveForward = false;
        this.moveBackward = false;
        this.moveLeft = false;
        this.moveRight = false;

        this.role = null;
    }
    // 是否锁定当前页面
    onPointerlockChange() {
        this.isLocked = document.pointerLockElement === this.domElement;
    }

    onPointerlockError() {
        console.error('THREE.PointerLockControls: Unable to use Pointer Lock API');
    }
    // 鼠标移动
    onMouseMove(event) {
        if (this.isLocked && this.role) {
            let movementX = event.movementX || event.mozMovementX || event.webkitMovementX || 0;
            let movementY = event.movementY || event.mozMovementY || event.webkitMovementY || 0;

            this.yawObject.rotation.y -= movementX * 0.002;

            this.pitchObject.rotation.x -= movementY * 0.002;
            this.pitchObject.rotation.x = Math.max(-Math.PI / 2, Math.min(Math.PI / 2, this.pitchObject.rotation.x));
            this.role.rotation.y -= movementX * 0.002;
            // 通过总线先Web3D界面发送玩家移动事件
            FirstPersonControls.bus.emit('modifyRole');
        }
    }
    // 监听按键按下
    onKeyDown(event) {
        switch (event.keyCode) {
            case KEY_W:
                this.moveForward = true;
                break;
            case KEY_A:
                this.moveLeft = true;
                break;
            case KEY_S:
                this.moveBackward = true;
                break;
            case KEY_D:
                this.moveRight = true;
                break;
        }
    }
    // 监听按键松开
    onKeyUp(event) {
        switch (event.keyCode) {
            case KEY_W:
                this.moveForward = false;
                break;
            case KEY_A:
                this.moveLeft = false;
                break;
            case KEY_S:
                this.moveBackward = false;
                break;
            case KEY_D:
                this.moveRight = false;
                break;
        }
    }

    update(delta) {

        // 移动速度
        const moveSpeed = 100;

        // 确定移动方向
        let direction = new THREE.Vector3();
        direction.x = Number(this.moveRight) - Number(this.moveLeft);
        direction.z = Number(this.moveBackward) - Number(this.moveForward);
        direction.y = 0;

        // 移动方向向量归一化，使得实际移动的速度大小不受方向影响
        if (direction.x !== 0 || direction.z !== 0) {
            direction.normalize();
        }

        // 移动距离等于速度乘上间隔时间delta
        if (this.moveForward || this.moveBackward) {
            this.yawObject.translateZ(moveSpeed * direction.z * delta);
            if (this.role) {
                this.role.translateZ(-moveSpeed * direction.z * delta);
            }
        }
        if (this.moveLeft || this.moveRight) {
            this.yawObject.translateX(moveSpeed * direction.x * delta);
            if (this.role)
                this.role.translateX(-moveSpeed * direction.x * delta);
        }
        if (this.role && (this.moveLeft || this.moveRight || this.moveBackward || this.moveForward)) {
            FirstPersonControls.bus.emit('modifyRole');
        }

    }

    connect() {
        this.domElement.addEventListener('click', this.domElement.requestPointerLock);
        document.addEventListener('pointerlockchange', this.onPointerlockChange.bind(this), false);
        document.addEventListener('pointerlockerror', this.onPointerlockError.bind(this), false);
        document.addEventListener('mousemove', this.onMouseMove.bind(this), false);
        //keybind
        document.addEventListener('keydown', this.onKeyDown.bind(this), false);
        document.addEventListener('keyup', this.onKeyUp.bind(this), false);
    }

    disconnect() {
        this.domElement.removeEventListener('click', this.domElement.requestPointerLock);
        document.removeEventListener('pointerlockchange', this.onPointerlockChange.bind(this), false);
        document.removeEventListener('pointerlockerror', this.onPointerlockError.bind(this), false);
        document.removeEventListener('mousemove', this.onMouseMove.bind(this), false);
        //keybind
        document.removeEventListener('keydown', this.onKeyDown.bind(this), false);
        document.removeEventListener('keyup', this.onKeyUp.bind(this), false);
    }
}

export default FirstPersonControls;
