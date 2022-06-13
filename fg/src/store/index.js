import {createStore} from 'vuex'

export default createStore({
    state: {
        token: localStorage.getItem('token') || null,
        username: localStorage.getItem('username') || null
    },
    mutations: {
        setToken(state, token) {
            state.token = token
            localStorage.setItem('token', token);
        },
        setUsername(state, username) {
            state.username = username;
            localStorage.setItem('username', username);
        }
    },
    actions: {},
    modules: {}
})
