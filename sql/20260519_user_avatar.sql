ALTER TABLE lab_user
    ADD COLUMN avatar_url VARCHAR(500) NULL COMMENT '头像地址' AFTER phone;
