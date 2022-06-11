import io from "socket.io-client";


const socket = io('ws://localhost:8081/')

export default socket