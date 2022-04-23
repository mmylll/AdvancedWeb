<template>
  <div class="room">
    <Web3D/>
    <Chat :messages="test" :is-show="isShow"/>
  </div>
</template>

<script>
import Web3D from "@/components/Web3D";
import Chat from "@/components/Chat";

export default {
  name: "Room",
  components: {Web3D, Chat},
  data() {
    return {
      isShow: false,
      test: [{
        user: '张甲源',
        content: 'hello , sir'
      }]
    }
  },
  methods: {

  },

  mounted() {

  }
}
</script>

<style scoped>
.room {
  position: relative;
}
</style>