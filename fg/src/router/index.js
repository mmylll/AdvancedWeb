import {createRouter, createWebHistory} from 'vue-router'
import Room from "@/views/Room";
import LogIn from "@/views/LogIn";
import About from "@/views/About"

const routes = [
    {
        path: '/room',
        name: 'Room',
        component: Room
    },
    {
        path: '/',
        name: 'Login',
        // route level code-splitting
        // this generates a separate chunk (about.[hash].js) for this route
        // which is lazy-loaded when the route is visited.
        component: Room
    },
    {
        path: '/about',
        name: 'About',
        component: About,
        meta: {
            keepAlive: 1
        }
    }
]

const router = createRouter({
    history: createWebHistory(process.env.BASE_URL),
    routes
})

export default router
