import {createApp} from 'vue'
import App from './App.vue'
import router from './router'
import store from './store'
import VueAxios from "vue-axios";
import axios from "axios";
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import mitt from "mitt";

axios.defaults.baseURL = "http://localhost:8080"
axios.interceptors.request.use((config) => {
    if (store.state.token != null) {
        config.headers.TOKEN = store.state.token
    }
    return config
}, error => {
    console.log(error)
})

axios.interceptors.response.use(response => {
    console.log(response)
    return response
}, error => {
    return Promise.reject(error)
})

const bus = mitt()
const app = createApp(App)
app.config.globalProperties.$bus = bus
app.use(store).use(router).use(ElementPlus).use(VueAxios, axios).mount('#app')
