package com.example.backend.service;

import com.alibaba.fastjson.JSONObject;
import com.corundumstudio.socketio.AckRequest;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.annotation.OnConnect;
import com.corundumstudio.socketio.annotation.OnDisconnect;
import com.corundumstudio.socketio.annotation.OnEvent;
import com.example.backend.model.*;
import com.example.backend.repository.UserLogRepository;
import com.example.backend.utils.UserLog;
import com.sun.nio.sctp.MessageInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SocketIOService {
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    // 用来存已连接的客户端
    private static final Map<String, SocketIOClient> clientMap = new ConcurrentHashMap<>();

    private final Room room = Room.getRoom();

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
     * Spring IoC容器在销毁SocketIOService Bean之前关闭,避免重启项目服务端口占用问题
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
    public void onJoin(SocketIOClient client, AckRequest ackRequest, JSONObject data) {
        String username = client.getHandshakeData().getSingleUrlParam("username");
        Player player = new Player();
        player.setUsername(username);
        setPlayerPosition(player, data);

        // 添加到玩家列表
        room.getPlayers().put(username, player);

        Map<String, Object> map = new HashMap<>();
        map.put("player", player);
        // 转发新加入的玩家给其他玩家
        sendToOthers("OnPlayerJoin", username, map);

        // 向数据库写入日志
        UserLog userLog = new UserLog(username, "join", LocalDateTime.now(), null);
        userLogRepository.save(userLog);
    }

    /**
     * 玩家离开房间的事件处理器
     * */
    @OnEvent(value="OnLeave")
    public void onLeave(SocketIOClient client, AckRequest ackRequest, JSONObject data) {
        String username = client.getHandshakeData().getSingleUrlParam("username");
        Player player = room.getPlayers().get(username);
        // 保存离开前的位置和朝向
        setPlayerPosition(player, data);

        Map<String, Object> map = new HashMap<>();
        map.put("username", username);
        // 转发离开的玩家名给其他玩家
        sendToOthers("OnPlayerLeave", username, map);

        // 向数据库写入日志
        UserLog userLog = new UserLog(username, "leave", LocalDateTime.now(), null);
        userLogRepository.save(userLog);
    }

    /**
     * 玩家更新位置的事件处理器
     * */
    @OnEvent(value="OnUpdate")
    public void onUpdate(SocketIOClient client, AckRequest ackRequest, JSONObject data) {
        String username = client.getHandshakeData().getSingleUrlParam("username");
        Player player = room.getPlayers().get(username);
        // 更新位置和朝向
        setPlayerPosition(player, data);

        Map<String, Object> map = new HashMap<>();
        map.put("player", player);
        // 转发被更新的玩家给其他玩家
        sendToOthers("OnPlayerUpdate", username, map);
    }

    /**
     * 玩家拿起汉诺塔的事件处理器
     * */
    @OnEvent(value="OnPickUp")
    public void onPickUp(SocketIOClient client, AckRequest ackRequest, JSONObject data) {
        String username = client.getHandshakeData().getSingleUrlParam("username");
        Player player = room.getPlayers().get(username);

        Integer plate = (Integer) data.get("index");
        player.setPlate(plate);

        Integer columnIndex = (Integer) data.get("columnIndex");
        Column column = room.getColumns().get(columnIndex);
        // 更新柱子的plates
        synchronized (room.getColumns().get(columnIndex)) {
            column.getPlates().remove(0);
        }

        Map<String, Object> map = new HashMap<>();
        map.put("username", username);
        map.put("index", plate);
        // 转发被更新的玩家给其他玩家
        sendToOthers("OnPlayerPickUp", username, map);
        // 向数据库写入日志
        UserLog userLog = new UserLog(username, "pickUp",LocalDateTime.now(), plate);
        userLogRepository.save(userLog);
    }

    /**
     * 玩家放下汉诺塔的事件处理器
     * */
    @OnEvent(value="OnPutDown")
    public void onPutDown(SocketIOClient client, AckRequest ackRequest, JSONObject data) {
        String username = client.getHandshakeData().getSingleUrlParam("username");
        Player player = room.getPlayers().get(username);

        Integer plateIndex = (Integer) data.get("index");
        Plate plate = new Plate(plateIndex, Plate.BASE_RADIUS + plateIndex * Plate.RADIUS_STEP, Plate.HEIGHT);
        player.setPlate(null);

        Integer columnIndex = (Integer) data.get("columnIndex");
        Column column = room.getColumns().get(columnIndex);
        // 更新柱子的plates
        synchronized (room.getColumns().get(columnIndex)) {
            column.getPlates().add(0,plate);
        }

        Map<String, Object> map = new HashMap<>();
        map.put("username", username);
        map.put("index", plateIndex);
        map.put("columnIndex",columnIndex);
        // 转发给其他玩家
        sendToOthers("OnPlayerPutDown", username, map);
        // 向数据库写入日志
        UserLog userLog = new UserLog(username, "putDown",LocalDateTime.now(), null);
        userLogRepository.save(userLog);
    }

    /**
     * 玩家发送聊天消息的事件处理器
     * */
    @OnEvent(value="OnSendMessage")
    public void onSendMessage(SocketIOClient client, AckRequest ackRequest, JSONObject data) {
        String username = client.getHandshakeData().getSingleUrlParam("username");
        String message = client.getHandshakeData().getSingleUrlParam("message");
        Map<String, Object> map = new HashMap<>();
        map.put("username", username);
        map.put("message", message);
        sendToOthers("OnPlayerSendMessage",username,map);
    }

    private void setPlayerPosition(Player player, JSONObject data) {
        Double x = (Double) data.get("x");
        Double y = (Double) data.get("y");
        Double z = (Double) data.get("z");
        Double rx = (Double) data.get("rx");
        Double ry = (Double) data.get("ry");
        Double rz = (Double) data.get("rz");
        player.setX(x);
        player.setY(y);
        player.setZ(z);
        player.setRx(rx);
        player.setRy(ry);
        player.setRz(rz);
    }

    private void sendToOthers(String event, String username, Map<String, Object> message) {
        clientMap.forEach((uname, socketIOClient) -> {
            if (!uname.equals(username)) {
                 socketIOClient.sendEvent(event, message);
            }
        });
    }
}
