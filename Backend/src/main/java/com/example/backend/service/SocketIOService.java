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

        /* 连接事件监听器 */
        socketIOServer.addConnectListener(client -> {
            log.info("-------------------连接");
            UUID uuid = client.getSessionId();
            if (uuid != null) {
                clientMap.put(uuid, client);
                log.info("客户端连接: uuid=" + uuid);
            }
            log.info("当前连接数:" + clientMap.size());
        });

        /* 断连事件监听器 */
        socketIOServer.addDisconnectListener(client -> {
            log.info("-------------------断开连接");
            UUID uuid = client.getSessionId();
            Map<UUID, Player> players;
            clientMap.remove(uuid);
            synchronized (room) {
                players = room.getPlayers();
            }
            String username = players.get(uuid).getUsername();

            players.remove(uuid);
            log.info("客户端断开连接: uuid=" + uuid);
            log.info("当前连接数:" + clientMap.size());
            log.info("剩余玩家数:" + players.size());
            client.disconnect();

            Map<String, Object> map = new HashMap<>();
            map.put("username", username);
            // 转发离开的玩家名给其他玩家
            sendToOthers("OnPlayerLeave", uuid, map);

            // 向数据库写入日志
            UserLog userLog = new UserLog(username, "leave", getLocalDateTime(), null);
            userLogRepository.save(userLog);
        });

        /* 加入房间事件监听器 */
        socketIOServer.addEventListener("OnJoin", JSONObject.class, (client, data, ackRequest) -> {
            log.info("-------------------加入");
            UUID uuid = client.getSessionId();
            Player player = new Player();
            String username = (String) data.get("username");
            player.setUsername((String) data.get("username"));
            setPlayerPosition(player, data);
            Map<UUID, Player> players;
            // 添加到玩家列表
            synchronized (room) {
                players = room.getPlayers();
            }

            players.put(uuid, player);
            log.info("玩家加入：" + player.getUsername());
            log.info("当前玩家数:" + players.size());

            Map<String, Object> map = new HashMap<>();
            map.put("player", player);
            // 转发新加入的玩家给其他玩家
            sendToOthers("OnPlayerJoin", uuid, map);
            // 向数据库写入日志
            UserLog userLog = new UserLog(username, "join", getLocalDateTime(), null);
            userLogRepository.save(userLog);
        });

        /* 更新模型事件监听器 */
        socketIOServer.addEventListener("OnUpdate", JSONObject.class, (client, data, ackRequest) -> {
            UUID uuid = client.getSessionId();
            String username = (String) data.get("username");
            Map<UUID, Player> players;
            synchronized (room) {
                players = room.getPlayers();
            }
            Player player = players.get(uuid);

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

        /* 拿起圆盘事件监听器 */
        socketIOServer.addEventListener("OnPickUp", JSONObject.class, (client, data, ackRequest) -> {
            log.info("-------------------拿起");
            UUID uuid = client.getSessionId();
            String username = (String) data.get("username");
            Map<UUID, Player> players;
            synchronized (room) {
                players = room.getPlayers();
            }
            Player player = players.get(uuid);

            if (player == null) {
                log.info("player is not found in player list");
                return;
            }

            Integer plateIndex = (Integer) data.get("index");
            player.setPlate(plateIndex);

            Integer columnIndex = (Integer) data.get("columnIndex");
            Plate plate = null;

            boolean someonePickedUp;
            // 更新柱子的plates
            synchronized (room) {
                Column column = room.getColumns().get(columnIndex);
                // 已经有人拿起plate，放弃pickUp并发回错误信息，释放锁之后结束处理
                someonePickedUp = room.isSomeonePickUp();
                if (someonePickedUp) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("state", 0);
                    client.sendEvent("PickedUp", map);
                    log.info("失败");
                } else {
                    // 否则，发回成功信息
                    int lastIndex = column.getPlates().size() - 1;
                    plate = column.getPlates().remove(lastIndex);
                    room.setSomeonePickUp(true);
                }
            }
            if (someonePickedUp)
                return;

            log.info("username:" + username + ", plateIndex:" + plateIndex + ", columnIndex:" + columnIndex);
            log.info("plate:" + plate);

            // 发回成功信息
            Map<String, Object> map = new HashMap<>();
            map.put("state", 1);
            client.sendEvent("PickedUp", map);
            log.info("成功");

            map.clear();
            map.put("username", username);
            map.put("index", plateIndex);
            map.put("columnIndex", columnIndex);
            map.put("plate", plate);
            // 转发被更新的玩家给其他玩家
            sendToOthers("OnPlayerPickUp", uuid, map);
            // 向数据库写入日志
            UserLog userLog = new UserLog(username, "pickUp", getLocalDateTime(), plateIndex);
            userLogRepository.save(userLog);
        });

        /* 放下圆盘事件监听器 */
        socketIOServer.addEventListener("OnPutDown", JSONObject.class, (client, data, ackRequest) -> {
            log.info("-------------------放下");

            UUID uuid = client.getSessionId();
            String username = (String) data.get("username");
            Map<UUID, Player> players;
            synchronized (room) {
                players = room.getPlayers();
            }

            Player player = players.get(uuid);
            if (player == null) {
                log.info("player is not found in player list");
                return;
            }

            Integer plateIndex = (Integer) data.get("index");
            Plate plate = new Plate(plateIndex, Plate.BASE_RADIUS + plateIndex * Plate.RADIUS_STEP, Plate.HEIGHT);
            player.setPlate(null);

            Integer columnIndex = (Integer) data.get("columnIndex");
            // 更新柱子的plates
            synchronized (room) {
                Column column = room.getColumns().get(columnIndex);
                column.getPlates().add(plate);
                room.setSomeonePickUp(false);
            }
            log.info("username:" + username + ", plateIndex:" + plateIndex + ", columnIndex:" + columnIndex);
            log.info("plate:" + plate);

            Map<String, Object> map = new HashMap<>();
            map.put("username", username);
            map.put("index", plateIndex);
            map.put("columnIndex", columnIndex);
            map.put("plate", plate);
            // 转发给其他玩家
            sendToOthers("OnPlayerPutDown", uuid, map);
            // 向数据库写入日志
            UserLog userLog = new UserLog(username, "putDown", getLocalDateTime(), plateIndex);
            userLogRepository.save(userLog);
        });

        /* 发送聊天消息事件监听器 */
        socketIOServer.addEventListener("OnSendMessage", JSONObject.class, (client, data, ackRequest) -> {
            log.info("-------------------发送消息");
            UUID uuid = client.getSessionId();
            String username = (String) data.get("username");
            String message = (String) data.get("message");
            log.info("玩家" + username + ": " + message);

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

    /**
     * 从前端数据对象中读取坐标和朝向信息，更新给定的Player模型实例
     * @param player 更新的目标对象
     * @param data 源数据对象
     * */
    private void setPlayerPosition(Player player, JSONObject data) {
        player.setX((Number) data.get("x"));
        player.setY((Number) data.get("y"));
        player.setZ((Number) data.get("z"));
        player.setRx((Number) data.get("rx"));
        player.setRy((Number) data.get("ry"));
        player.setRz((Number) data.get("rz"));
    }

    /**
     * 将给定的消息对象通过指定的事件发送给除了uuid对应的socketClient以外的其他连接
     * @param event 事件名称
     * @param uuid  不需要发送的用户
     * @param message 发送的消息对象
     * */
    private void sendToOthers(String event, UUID uuid, Map<String, Object> message) {
        clientMap.forEach((_uuid, socketIOClient) -> {
            if (!_uuid.equals(uuid)) {
                socketIOClient.sendEvent(event, message);
            }
        });
    }

    /**
     * 获取当前上海时区的LocalDateTime
     * */
    private LocalDateTime getLocalDateTime() {
        LocalDateTime now = LocalDateTime.now();
        ZoneId zoneId = ZoneId.of("Asia/Shanghai");
        return now.atZone(zoneId).toLocalDateTime();
    }
}
