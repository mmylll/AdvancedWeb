<template>
  <div class="chat">
    <div :class="{'chat-window':true,'selected':isShow}">
      <p v-for="(message,index) in messages" :id="index"><strong>{{ message.user }}:</strong>{{ message.content }}</p>
    </div>
    <input type="text" v-show="isShow" v-model="message" ref="messageInput">
  </div>

</template>

<script>
import socket from "@/socket";
export default {
  name: "Chat",
  data() {
    return {
      messages: [],
      isShow: false,
      message: ''
    }
  },
  methods: {
    onKeyBoard() {
      document.onkeydown = (e) => {
        if (e.code === 'Enter') {
          this.isShow = !this.isShow
          if (this.isShow) {
            this.$refs.messageInput.focus()
          }
          if (!this.isShow && this.message != '') {
            // 发送消息
            socket.emit('OnSendMessage', this.message);
            this.message = '';
          }
        }
      }
    },
  },
  mounted() {
    this.onKeyBoard()
    // 接收消息
    socket.on('OnPlayerSendMessage', (message) => {
      this.messages.push(message);
    })
  }
}
</script>

<style scoped>
.chat {
  width: 30%;
  max-height: 23%;
  min-height: 10%;
  position: absolute;
  left: 30px;
  top: 500px;
  z-index: 1000;
  background-color: rgba(0, 0, 0, 0);
}

.selected {
  background-color: rgba(109, 112, 106, 0.6);
  border-radius: 3px;
}

.chat-window {
  min-height: 80px;
  padding: 10px;
}

input {
  z-index: 1000;
  width: 97%;
  margin: 3px 0;
  background-color: rgba(109, 112, 106, 0.6);
  line-height: 25px;
  font-size: 17px;
  padding: 4px;
  outline: none;
  border: #2c3e50 1px solid;
  border-radius: 3px;
  color: #eadcdc;
}

.chat-window:hover {
  background-color: rgba(109, 112, 106, 0.6);
  border-radius: 3px;
}


.chat-window p {
  text-align: left;
  margin: 5px 0;

}

.chat-window p strong {
  font-size: 15px;
}
</style>
