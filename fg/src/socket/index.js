//import io from "socket.io-client";

import io from "socket.io-client";

const socket = io.connect('ws://121.4.80.83：7697/', {
    autoConnect: false,
    transports: ['websocket']
});

// api 7843->8080
// ws 7697 ->8000->nginx转发到10246
export default socket