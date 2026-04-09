-- 1. 创建 user 表
CREATE TABLE IF NOT EXISTS `user` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `username` VARCHAR(50) NOT NULL,
  `password` VARCHAR(255) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `username` (`username`)
);

-- 2. 修改 sample 表
ALTER TABLE `sample` DROP COLUMN IF EXISTS `uploaded_by`;
ALTER TABLE `sample` ADD COLUMN `user_id` INT NOT NULL AFTER `id`;
ALTER TABLE `sample` ADD COLUMN `sampling_data` TEXT;
ALTER TABLE `sample` ADD COLUMN `upload_format` VARCHAR(50);