-- 1. 创建 user 表
CREATE TABLE IF NOT EXISTS `user` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `username` VARCHAR(100) NOT NULL,
  `password_hash` VARCHAR(200) NOT NULL,
  `created_at` DATETIME NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `user_username_uindex` (`username`)
);

-- 2. 修改 sample 表
ALTER TABLE `sample` ADD COLUMN `user_id` INT NOT NULL DEFAULT 0 AFTER `id`;
ALTER TABLE `sample` ADD COLUMN `input_type` VARCHAR(50) NULL;
ALTER TABLE `sample` ADD COLUMN `file_name` TEXT NULL;
ALTER TABLE `sample` ADD COLUMN `parse_status` VARCHAR(50) NULL;
