DROP TABLE IF EXISTS hanoi_tower.log;
DROP TABLE IF EXISTS hanoi_tower.user;

CREATE TABLE IF NOT EXISTS hanoi_tower.user
(
    id          INT(11)             NOT NULL AUTO_INCREMENT,
    username    VARCHAR(20)         NOT NULL,
    password    VARCHAR(20)         NOT NULL,
    email       VARCHAR(20)         NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT user_id_uindex UNIQUE (id),
    CONSTRAINT user_username_uindex UNIQUE (username),
    INDEX (username)
)
    COMMENT '用户表';

INSERT INTO hanoi_tower.user
VALUES (1, 'administer', '123456', '111@fudan.edu.cn');

CREATE TABLE IF NOT EXISTS hanoi_tower.log
(
    id          INT(11)             NOT NULL AUTO_INCREMENT,
    username    VARCHAR(20)         NOT NULL,
    type        VARCHAR(20)         NOT NULL,
    date        DATETIME            NOT NULL,
    plate       INT(11)             DEFAULT NULL,
    PRIMARY KEY (id),
    CONSTRAINT log_id_index UNIQUE (id),
    CONSTRAINT log_username_foreign FOREIGN KEY (username) REFERENCES user(username)
)
    COMMENT '日志表';
