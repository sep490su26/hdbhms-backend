CREATE TABLE floor_plan_items
(
    id            BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    property_id   BIGINT UNSIGNED NOT NULL,
    floor_id      BIGINT UNSIGNED NOT NULL,
    room_id       BIGINT UNSIGNED NULL,
    item_type     VARCHAR(50)     NOT NULL,
    label         VARCHAR(255)    NULL,
    x             DECIMAL(10, 2)  NOT NULL,
    y             DECIMAL(10, 2)  NOT NULL,
    width         DECIMAL(10, 2)  NOT NULL,
    height        DECIMAL(10, 2)  NOT NULL,
    rotation      DECIMAL(10, 2)  NOT NULL DEFAULT 0,
    sort_order    INT             NOT NULL DEFAULT 0,
    metadata_json JSON            NULL,
    created_at    DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at    DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    KEY idx_floor_plan_items_property_floor (property_id, floor_id),
    KEY idx_floor_plan_items_room (room_id),
    CONSTRAINT fk_floor_plan_items_property FOREIGN KEY (property_id) REFERENCES properties (id),
    CONSTRAINT fk_floor_plan_items_floor FOREIGN KEY (floor_id) REFERENCES floors (id),
    CONSTRAINT fk_floor_plan_items_room FOREIGN KEY (room_id) REFERENCES rooms (id),
    CONSTRAINT chk_floor_plan_items_size CHECK (width > 0 AND height > 0)
) ENGINE = InnoDB;
