//import io from "socket.io-client";

import io from "socket.io-client";

const socket = io.connect('ws://localhost:10246/', {autoConnect: false});
export default socket