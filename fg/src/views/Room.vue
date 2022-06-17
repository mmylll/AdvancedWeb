<template>
  <div class="room">
    <Web3D/>
    <Chat/>
  </div>
</template>

<script>
  // 展示3D场景和对话框
import Web3D from "@/components/Web3D";
import Chat from "@/components/Chat";

export default {
  name: "Room",
  components: {Web3D, Chat},
  data() {
    return {}
  },
  methods: {},

  mounted() {

  }
}
</script>

<style scoped>
.room {
  position: relative;
}
</style>
