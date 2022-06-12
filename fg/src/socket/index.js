import io from "socket.io-client";


const socket = io('ws://localhost:10246/')

export default socket