package com.example.backend.service;

import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.annotation.OnConnect;
import com.corundumstudio.socketio.annotation.OnDisconnect;
import com.corundumstudio.socketio.annotation.OnEvent;
import com.example.backend.model.Message;
import com.example.backend.repository.UserLogRepository;
import com.example.backend.utils.UserLog;
import com.sun.nio.sctp.MessageInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SocketIOService {
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    // 用来存已连接的客户端
    private static final Map<String, SocketIOClient> clientMap = new ConcurrentHashMap<>();

    @Autowired
    private SocketIOServer socketIOServer;

    @Autowired
    private UserLogRepository userLogRepository;

    /**
     * Spring IoC容器创建之后，在加载SocketIOService Bean之后启动
     */
    @PostConstruct
    private void autoStartup() throws Exception {
        socketIOServer.start();
        log.info("socket.io初始化服务完成");
    }

    /**
     * Spring IoC容器在销毁SocketIOServiceImpl Bean之前关闭,避免重启项目服务端口占用问题
     */
    @PreDestroy
    private void autoStop() throws Exception {
        if (socketIOServer != null) {
            socketIOServer.stop();
            socketIOServer = null;
        }
        log.info("socket.io服务已关闭");
    }

    @OnConnect
    public void onConnect(SocketIOClient client) {
        String username = client.getHandshakeData().getSingleUrlParam("username");
        if (username != null && !username.isEmpty()) {
            clientMap.put(username, client);
            log.info("客户端连接: username=" + username);
        }
    }

    @OnDisconnect
    public void onDisconnect(SocketIOClient client) {
        String username = client.getHandshakeData().getSingleUrlParam("username");
        if (username != null && !username.isEmpty()) {
            clientMap.remove(username);
            client.disconnect();
        }
        log.info("客户端断开连接: username=" + username);
    }

    /**
     * 玩家加入房间的事件处理器
     * */
    @OnEvent(value="OnJoin")
    public void onJoin(SocketIOClient client) {
        String username = client.getHandshakeData().getSingleUrlParam("username");
        // Player player = playerList.get(username);
        String x = client.getHandshakeData().getSingleUrlParam("x");
        // player.setX(x);
        String y = client.getHandshakeData().getSingleUrlParam("y");
        // player.setY(y);
        String z = client.getHandshakeData().getSingleUrlParam("z");
        // player.setZ(z);
        String rx = client.getHandshakeData().getSingleUrlParam("rx");
        // player.setRx(rx);
        String ry = client.getHandshakeData().getSingleUrlParam("ry");
        // player.setRy(ry);
        String rz = client.getHandshakeData().getSingleUrlParam("rz");
        // player.setRz(rz);
        // 转发被更新的玩家给其他玩家
         clientMap.forEach((uname, socketIOClient) -> {
             if (!uname.equals(username)) {
                 // socketIOClient.sendEvent("OnPlayerJoin", player);
             }
         });
        // 向数据库写入日志... insertLog(username, type, plate)
        // insertLog(username, "join", null);
    }

    /**
     * 玩家离开房间的事件处理器
     * */
    @OnEvent(value="OnLeave")
    public void onLeave(SocketIOClient client) {

    }

    /**
     * 玩家更新位置的事件处理器
     * */
    @OnEvent(value="OnUpdate")
    public void onUpdate(SocketIOClient client) {

    }

    /**
     * 玩家拿起汉诺塔的事件处理器
     * */
    @OnEvent(value="OnPickUp")
    public void onPickUp(SocketIOClient client) {

    }

    /**
     * 玩家放下汉诺塔的事件处理器
     * */
    @OnEvent(value="OnPutDown")
    public void onPutDown(SocketIOClient client) {

    }

    /**
     * 玩家发送聊天消息的事件处理器
     * */
    @OnEvent(value="OnSendMessage")
    public void onSendMessage(SocketIOClient client) {

    }
}
