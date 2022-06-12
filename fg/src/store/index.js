import {createStore} from 'vuex'

export default createStore({
    state: {
        token: localStorage.getItem('token') || null,
        username: ''
    },
    mutations: {
        setToken(state, token) {
            state.token = token
            localStorage.setItem('token', token);
        }
    },
    actions: {},
    modules: {}
})
