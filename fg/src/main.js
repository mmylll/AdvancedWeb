import {createApp} from 'vue'
import App from './App.vue'
import router from './router'
import store from './store'
import VueAxios from "vue-axios";
import axios from "axios";
import ElementPlus from 'element-plus'
import  'element-plus/dist/index.css'

axios.defaults.baseURL = "http://back:8080"
axios.interceptors.request.use((config) => {
    if (store.state.token != null) {
        config.headers.token = store.state.token
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




createApp(App).use(store).use(router).use(ElementPlus).use(VueAxios, axios).mount('#app')
