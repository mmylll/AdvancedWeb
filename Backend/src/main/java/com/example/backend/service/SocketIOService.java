package com.example.backend.service;

import com.alibaba.fastjson.JSONObject;
import com.corundumstudio.socketio.AckRequest;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.annotation.OnConnect;
import com.corundumstudio.socketio.annotation.OnDisconnect;
import com.corundumstudio.socketio.annotation.OnEvent;
import com.corundumstudio.socketio.listener.ConnectListener;
import com.corundumstudio.socketio.listener.DataListener;
import com.example.backend.model.Column;
import com.example.backend.model.Plate;
import com.example.backend.model.Player;
import com.example.backend.model.Room;
import com.example.backend.repository.UserLogRepository;
import com.example.backend.utils.UserLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.time.LocalDateTime;
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
            UUID uuid = client.getSessionId();
            if (uuid != null) {
                clientMap.put(uuid, client);
                log.info("客户端连接: uuid=" + uuid);
            }
        });


        socketIOServer.addDisconnectListener(client -> {
            UUID uuid = client.getSessionId();
            if (uuid != null) {
                clientMap.remove(uuid);
                client.disconnect();
            }
            log.info("客户端断开连接: uuid=" + uuid);
        });

        socketIOServer.addEventListener("OnJoin", JSONObject.class, (client, data, ackRequest) -> {
            UUID uuid = client.getSessionId();
            Player player = new Player();
            String username = (String) data.get("username");
            player.setUsername((String) data.get("username"));
            setPlayerPosition(player, data);

            // 添加到玩家列表
            synchronized (room) {
                room.getPlayers().add(player);
            }
            Map<String, Object> map = new HashMap<>();
            map.put("player", player);
            // 转发新加入的玩家给其他玩家
            sendToOthers("OnPlayerJoin", uuid, map);

            // 向数据库写入日志
            UserLog userLog = new UserLog(username, "join", LocalDateTime.now(), null);
            userLogRepository.save(userLog);
        });

        socketIOServer.addEventListener("OnLeave", JSONObject.class, (client, data, ackRequest) -> {
            UUID uuid = client.getSessionId();
            String username = (String) data.get("username");
            Player player = null;
            synchronized (room) {
                for (Player p : room.getPlayers()) {
                    if (p.getUsername().equals(username))
                        player = p;
                }
            }
            if (player == null) {
                log.info("player is not found in player list");
                return;
            }
            // 保存离开前的位置和朝向
            setPlayerPosition(player, data);

            Map<String, Object> map = new HashMap<>();
            map.put("username", username);
            // 转发离开的玩家名给其他玩家
            sendToOthers("OnPlayerLeave", uuid, map);

            // 向数据库写入日志
            UserLog userLog = new UserLog(username, "leave", LocalDateTime.now(), null);
            userLogRepository.save(userLog);
        });

        socketIOServer.addEventListener("OnUpdate", JSONObject.class, (client, data, ackRequest) -> {
            UUID uuid = client.getSessionId();
            String username = (String) data.get("username");
            Player player = null;
            synchronized (room) {
                for (Player p : room.getPlayers()) {
                    if (p.getUsername().equals(username))
                        player = p;
                }
            }
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
            UUID uuid = client.getSessionId();
            String username = (String) data.get("username");
            Player player = null;
            synchronized (room) {
                for (Player p : room.getPlayers()) {
                    if (p.getUsername().equals(username))
                        player = p;
                }
            }
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
            // 转发被更新的玩家给其他玩家
            sendToOthers("OnPlayerPickUp", uuid, map);
            // 向数据库写入日志
            UserLog userLog = new UserLog(username, "pickUp", LocalDateTime.now(), plate);
            userLogRepository.save(userLog);
        });

        socketIOServer.addEventListener("OnPutDown", JSONObject.class, (client, data, ackRequest) -> {
            UUID uuid = client.getSessionId();
            String username = (String) data.get("username");
            Player player = null;
            synchronized (room) {
                for (Player p : room.getPlayers()) {
                    if (p.getUsername().equals(username))
                        player = p;
                }
            }
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
            UserLog userLog = new UserLog(username, "putDown", LocalDateTime.now(), null);
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

    @OnConnect
    public void onConnect(SocketIOClient client) {
        UUID uuid = client.getSessionId();
        if (uuid != null) {
            clientMap.put(client.getSessionId(), client);
            log.info("客户端连接: uuid=" + uuid);
        }
    }

    @OnDisconnect
    public void onDisconnect(SocketIOClient client) {
        UUID uuid = client.getSessionId();
        if (uuid != null) {
            clientMap.remove(uuid);
            client.disconnect();
        }
        log.info("客户端断开连接: uuid=" + uuid);
    }

    /**
     * 玩家加入房间的事件处理器
     */
    @OnEvent(value = "OnJoin")
    public void onJoin(SocketIOClient client, AckRequest ackRequest, JSONObject data) {
        UUID uuid = client.getSessionId();
        String username = (String) data.get("username");
        Player player = new Player();
        player.setUsername(username);
        setPlayerPosition(player, data);

        // 添加到玩家列表
        synchronized (room) {
            room.getPlayers().add(player);
        }
        Map<String, Object> map = new HashMap<>();
        map.put("player", player);
        // 转发新加入的玩家给其他玩家
        sendToOthers("OnPlayerJoin", uuid, map);

        // 向数据库写入日志
        UserLog userLog = new UserLog(username, "join", LocalDateTime.now(), null);
        userLogRepository.save(userLog);
    }

    /**
     * 玩家离开房间的事件处理器
     */
    @OnEvent(value = "OnLeave")
    public void onLeave(SocketIOClient client, AckRequest ackRequest, JSONObject data) {
        UUID uuid = client.getSessionId();
        String username = (String) data.get("username");
        Player player = null;
        synchronized (room) {
            for (Player p : room.getPlayers()) {
                if (p.getUsername().equals(username))
                    player = p;
            }
        }
        if (player == null) {
            log.info("player is not found in player list");
            return;
        }
        // 保存离开前的位置和朝向
        setPlayerPosition(player, data);

        Map<String, Object> map = new HashMap<>();
        map.put("username", username);
        // 转发离开的玩家名给其他玩家
        sendToOthers("OnPlayerLeave", uuid, map);

        // 向数据库写入日志
        UserLog userLog = new UserLog(username, "leave", LocalDateTime.now(), null);
        userLogRepository.save(userLog);
    }

    /**
     * 玩家更新位置的事件处理器
     */
    @OnEvent(value = "OnUpdate")
    public void onUpdate(SocketIOClient client, AckRequest ackRequest, JSONObject data) {
        UUID uuid = client.getSessionId();
        String username = (String) data.get("username");
        Player player = null;
        synchronized (room) {
            for (Player p : room.getPlayers()) {
                if (p.getUsername().equals(username))
                    player = p;
            }
        }
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
    }

    /**
     * 玩家拿起汉诺塔的事件处理器
     */
    @OnEvent(value = "OnPickUp")
    public void onPickUp(SocketIOClient client, AckRequest ackRequest, JSONObject data) {
        UUID uuid = client.getSessionId();
        String username = (String) data.get("username");
        Player player = null;
        synchronized (room) {
            for (Player p : room.getPlayers()) {
                if (p.getUsername().equals(username))
                    player = p;
            }
        }
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
        // 转发被更新的玩家给其他玩家
        sendToOthers("OnPlayerPickUp", uuid, map);
        // 向数据库写入日志
        UserLog userLog = new UserLog(username, "pickUp", LocalDateTime.now(), plate);
        userLogRepository.save(userLog);
    }

    /**
     * 玩家放下汉诺塔的事件处理器
     */
    @OnEvent(value = "OnPutDown")
    public void onPutDown(SocketIOClient client, AckRequest ackRequest, JSONObject data) {
        UUID uuid = client.getSessionId();
        String username = (String) data.get("username");
        Player player = null;
        synchronized (room) {
            for (Player p : room.getPlayers()) {
                if (p.getUsername().equals(username))
                    player = p;
            }
        }
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
        UserLog userLog = new UserLog(username, "putDown", LocalDateTime.now(), null);
        userLogRepository.save(userLog);
    }

    /**
     * 玩家发送聊天消息的事件处理器
     */
    @OnEvent(value = "OnSendMessage")
    public void onSendMessage(SocketIOClient client, AckRequest ackRequest, JSONObject data) {
        UUID uuid = client.getSessionId();
        String username = (String) data.get("username");
        String message = client.getHandshakeData().getSingleUrlParam("message");
        Map<String, Object> map = new HashMap<>();
        map.put("username", username);
        map.put("message", message);
        sendToOthers("OnPlayerSendMessage", uuid, map);
    }

    private void setPlayerPosition(Player player, JSONObject data) {
//        Double x = ((Number) data.get("x")).doubleValue();
//        Double y = (Integer) data.get("y") * 1.0;
//        Double z = ((Number) data.get("z")).doubleValue();
//        Double rx = (Integer) data.get("rx") * 1.0;
//        Double ry = ((Number) data.get("ry")).doubleValue();
//        Double rz = (Integer) data.get("rz") * 1.0;
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
}
