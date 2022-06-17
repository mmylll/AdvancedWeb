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
import java.util.List;
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
                log.info("客户端连接: uuid = " + uuid);
            }
            log.info("当前连接数:" + clientMap.size());
        });

        /* 断连事件监听器 */
        socketIOServer.addDisconnectListener(client -> {
            log.info("-------------------断开连接");
            UUID uuid = client.getSessionId();
            Map<UUID, Player> players;
            synchronized (room) {
                players = room.getPlayers();
            }

            clientMap.remove(uuid);
            String username = players.get(uuid).getUsername();
            players.remove(uuid);
            client.disconnect();

            log.info("客户端断开连接: uuid = " + uuid + ", username = " + username);
            log.info("当前连接数:" + clientMap.size());
            log.info("剩余玩家数:" + players.size());

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
            String username = (String) data.get("username");
            Map<UUID, Player> players;
            // 添加到玩家列表
            synchronized (room) {
                players = room.getPlayers();
            }

            Player player = new Player();
            player.setUsername(username);
            setPlayerPosition(player, data);
            players.put(uuid, player);

            log.info("玩家加入：uuid = " + uuid + ", username = " + username);
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
            Integer plateIndex = (Integer) data.get("index");
            Integer columnIndex = (Integer) data.get("columnIndex");
            Map<UUID, Player> players;
            boolean someonePickedUp;

            synchronized (room) {
                // 获取player，确保想要拿起盘子的player存在
                players = room.getPlayers();
                Player player = players.get(uuid);
                if (player == null) {
                    log.info("player not found: uuid = " + uuid);
                    return;
                }

                Column column = room.getColumns().get(columnIndex);
                // 已经有人拿起plate，放弃pickUp并发回错误信息，释放锁之后结束处理
                someonePickedUp = room.isSomeonePickUp();
                List<Plate> platesOnColumn = column.getPlates();
                // 没有任何人拿着圆盘且柱子上有圆盘可拿，则成功拿起
                if (!someonePickedUp && player.getPlate() == null && platesOnColumn.size() > 0) {
                    // 更新柱子上的plates
                    platesOnColumn.remove(platesOnColumn.size() - 1);
                    // 更新player持有的plate
                    player.setPlate(plateIndex);
                    // 现在有人拿着圆盘
                    room.setSomeonePickUp(true);

                    Map<String, Object> map = new HashMap<>();
                    map.put("state", 1);
                    // 返回客户端成功消息
                    client.sendEvent("PickedUp", uuid, map);

                    // 转发被更新的玩家给其他玩家
                    map.put("columns", room.getColumns());
                    sendToOthers("OnPlayerPickUp", uuid, map);

                    // 向数据库写入日志
                    UserLog userLog = new UserLog(username, "pickUp", getLocalDateTime(), plateIndex);
                    userLogRepository.save(userLog);
                    log.info("成功");
                    log.info("username:" + username + ", plateIndex:" + plateIndex + ", columnIndex:" + columnIndex);
                }
                // 否则，不能拿起
                else {
                    Map<String, Object> map = new HashMap<>();
                    map.put("state", 0);
                    // 返回客户端失败消息
                    client.sendEvent("PickedUp", uuid, map);
                    log.info("失败");
                }
            }
        });

        /* 放下圆盘事件监听器 */
        socketIOServer.addEventListener("OnPutDown", JSONObject.class, (client, data, ackRequest) -> {
            log.info("-------------------放下");
            UUID uuid = client.getSessionId();
            String username = (String) data.get("username");
            Integer plateIndex = (Integer) data.get("index");
            Integer columnIndex = (Integer) data.get("columnIndex");
            Map<UUID, Player> players;

            Plate plate = new Plate(plateIndex, Plate.BASE_RADIUS + plateIndex * Plate.RADIUS_STEP, Plate.HEIGHT);

            synchronized (room) {
                players = room.getPlayers();
                // 获取player，确保想要放下圆盘的player存在
                Player player = players.get(uuid);
                if (player == null) {
                    log.info("player not found: uuid = " + uuid);
                    return;
                }

                Integer holdingPlate = player.getPlate();
                Column column = room.getColumns().get(columnIndex);
                List<Plate> platesOnColumn = column.getPlates();
                // 1. 有player拿着圆盘且那个人是自己;2. 将放下的圆盘编号小于柱子顶部的圆盘
                if (room.isSomeonePickUp() && holdingPlate != null && holdingPlate.equals(plateIndex) &&
                        plateIndex < platesOnColumn.get(platesOnColumn.size() - 1).getIndex()) {
                    // 更新柱子的plates
                    platesOnColumn.add(plate);
                    // 更新player持有的plate
                    player.setPlate(null);
                    // 现在没有任何人拿着圆盘
                    room.setSomeonePickUp(false);

                    log.info("username:" + username + ", plateIndex:" + plateIndex + ", columnIndex:" + columnIndex);
                    log.info("plate:" + plate);

                    Map<String, Object> map = new HashMap<>();
                    map.put("columns", room.getColumns());
                    // 转发给其他玩家
                    sendToOthers("OnPlayerPutDown", uuid, map);
                    // 向数据库写入日志
                    UserLog userLog = new UserLog(username, "putDown", getLocalDateTime(), plateIndex);
                    userLogRepository.save(userLog);
                }
                else {
                    log.info("失败");
                }
            }
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
