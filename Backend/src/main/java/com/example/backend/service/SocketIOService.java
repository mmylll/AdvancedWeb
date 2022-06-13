package com.example.backend.service;

import com.alibaba.fastjson.JSONObject;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.example.backend.model.Column;
import com.example.backend.model.Plate;
import com.example.backend.model.Player;
import com.example.backend.model.Room;
import com.example.backend.repository.UserLogRepository;
import com.example.backend.utils.UserLog;
import org.apache.tomcat.jni.Local;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SocketIOService {
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    // 用来存已连接的客户端
    private static final Map<UUID, SocketIOClient> clientMap = new ConcurrentHashMap<>();

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

        socketIOServer.addConnectListener(client -> {
            System.out.println("-------------------连接");
            UUID uuid = client.getSessionId();
            if (uuid != null) {
                clientMap.put(uuid, client);
                log.info("客户端连接: uuid=" + uuid);
            }
            log.info("当前连接数:" + clientMap.size());
        });


        socketIOServer.addDisconnectListener(client -> {
            UUID uuid = client.getSessionId();
            String username = room.getPlayers().get(uuid).getUsername();
            if (uuid != null) {
                clientMap.remove(uuid);
                room.getPlayers().remove(uuid);
                client.disconnect();
            }
            log.info("客户端断开连接: uuid=" + uuid);
            log.info("当前连接数:" + clientMap.size());
            log.info("剩余玩家数:" + room.getPlayers().size());

            Map<String, Object> map = new HashMap<>();
            map.put("username", username);
            // 转发离开的玩家名给其他玩家
            sendToOthers("OnPlayerLeave", uuid, map);

            // 向数据库写入日志
            UserLog userLog = new UserLog(username, "leave", getLocalDateTime(), null);
            userLogRepository.save(userLog);
        });

        socketIOServer.addEventListener("OnJoin", JSONObject.class, (client, data, ackRequest) -> {
            UUID uuid = client.getSessionId();
            Player player = new Player();
            String username = (String) data.get("username");
            player.setUsername((String) data.get("username"));
            setPlayerPosition(player, data);

            // 添加到玩家列表
            room.getPlayers().put(uuid, player);
            Map<String, Object> map = new HashMap<>();
            map.put("player", player);
            // 转发新加入的玩家给其他玩家
            sendToOthers("OnPlayerJoin", uuid, map);

            // 向数据库写入日志
            UserLog userLog = new UserLog(username, "join", getLocalDateTime(), null);
            userLogRepository.save(userLog);
        });

        socketIOServer.addEventListener("OnLeave", JSONObject.class, (client, data, ackRequest) -> {
            UUID uuid = client.getSessionId();
            String username = (String) data.get("username");
            Player player = room.getPlayers().get(uuid);
            if (player == null) {
                log.info("player is not found in player list");
                return;
            }
            room.getPlayers().remove(uuid);
            log.info("玩家离开：" + player.getUsername());

            Map<String, Object> map = new HashMap<>();
            map.put("username", username);
            // 转发离开的玩家名给其他玩家
            sendToOthers("OnPlayerLeave", uuid, map);

            // 向数据库写入日志
            UserLog userLog = new UserLog(username, "leave", getLocalDateTime(), null);
            userLogRepository.save(userLog);
        });

        socketIOServer.addEventListener("OnUpdate", JSONObject.class, (client, data, ackRequest) -> {
            UUID uuid = client.getSessionId();
            String username = (String) data.get("username");
            Player player = room.getPlayers().get(uuid);
            if (player == null) {
                log.info("player is not found in player list");
                return;
            }
            // 更新位置和朝向
            setPlayerPosition(player, data);

            Map<String, Object> map = new HashMap<>();
            map.put("player", player);
            // 转发被更新的玩家给其他玩家
            sendToOthers("OnPlayerUpdate", uuid, map);
        });

        socketIOServer.addEventListener("OnPickUp", JSONObject.class, (client, data, ackRequest) -> {
            System.out.println("--------------------------onpickup");

            UUID uuid = client.getSessionId();
            String username = (String) data.get("username");
            Player player = room.getPlayers().get(uuid);
            if (player == null) {
                log.info("player is not found in player list");
                return;
            }

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
            map.put("columnIndex",columnIndex);
            // 转发被更新的玩家给其他玩家
            sendToOthers("OnPlayerPickUp", uuid, map);
            // 向数据库写入日志
            UserLog userLog = new UserLog(username, "pickUp", getLocalDateTime(), plate);
            userLogRepository.save(userLog);
        });

        socketIOServer.addEventListener("OnPutDown", JSONObject.class, (client, data, ackRequest) -> {
            System.out.println("--------------------------onputdown");


            UUID uuid = client.getSessionId();
            String username = (String) data.get("username");
            Player player = room.getPlayers().get(uuid);
            if (player == null) {
                log.info("player is not found in player list");
                return;
            }

            Integer plateIndex = (Integer) data.get("index");
            Plate plate = new Plate(plateIndex, Plate.BASE_RADIUS + plateIndex * Plate.RADIUS_STEP, Plate.HEIGHT);
            player.setPlate(null);

            Integer columnIndex = (Integer) data.get("columnIndex");
            Column column = room.getColumns().get(columnIndex);
            // 更新柱子的plates
            synchronized (room.getColumns().get(columnIndex)) {
                column.getPlates().add(0, plate);
            }

            Map<String, Object> map = new HashMap<>();
            map.put("username", username);
            map.put("index", plateIndex);
            map.put("columnIndex", columnIndex);
            // 转发给其他玩家
            sendToOthers("OnPlayerPutDown", uuid, map);
            // 向数据库写入日志
            UserLog userLog = new UserLog(username, "putDown", getLocalDateTime(), null);
            userLogRepository.save(userLog);
        });

        socketIOServer.addEventListener("OnSendMessage", JSONObject.class, (client, data, ackRequest) -> {
            UUID uuid = client.getSessionId();
            String username = (String) data.get("username");
            String message = client.getHandshakeData().getSingleUrlParam("message");
            Map<String, Object> map = new HashMap<>();
            map.put("username", username);
            map.put("message", message);
            sendToOthers("OnPlayerSendMessage", uuid, map);
        });

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

    private void setPlayerPosition(Player player, JSONObject data) {
        player.setX((Number) data.get("x"));
        player.setY((Number) data.get("y"));
        player.setZ((Number) data.get("z"));
        player.setRx((Number) data.get("rx"));
        player.setRy((Number) data.get("ry"));
        player.setRz((Number) data.get("rz"));
    }

    private void sendToOthers(String event, UUID uuid, Map<String, Object> message) {
        clientMap.forEach((_uuid, socketIOClient) -> {
            if (!_uuid.equals(uuid)) {
                socketIOClient.sendEvent(event, message);
            }
        });
    }

    private LocalDateTime getLocalDateTime() {
        LocalDateTime now = LocalDateTime.now();
        ZoneId zoneId = ZoneId.of("Asia/Shanghai");
        return now.atZone(zoneId).toLocalDateTime();
    }
}
