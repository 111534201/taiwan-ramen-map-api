-- phpMyAdmin SQL Dump
-- version 5.2.1
-- https://www.phpmyadmin.net/
--
-- 主機： 127.0.0.1
-- 產生時間： 2025-04-17 03:09:59
-- 伺服器版本： 10.4.32-MariaDB
-- PHP 版本： 8.2.12

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- 資料庫： `ramen_map_db`
--

-- --------------------------------------------------------

--
-- 資料表結構 `events`
--

CREATE TABLE `events` (
  `id` bigint(20) NOT NULL,
  `content` tinytext NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `shop_id` bigint(20) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- 資料表結構 `event_media`
--

CREATE TABLE `event_media` (
  `id` bigint(20) NOT NULL,
  `event_id` bigint(20) NOT NULL,
  `url` varchar(1024) NOT NULL,
  `type` varchar(20) DEFAULT 'IMAGE'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- 資料表結構 `reviews`
--

CREATE TABLE `reviews` (
  `id` bigint(20) NOT NULL,
  `content` text DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `rating` int(11) DEFAULT NULL,
  `reply_count` int(11) DEFAULT 0,
  `updated_at` datetime(6) DEFAULT NULL,
  `parent_review_id` bigint(20) DEFAULT NULL,
  `shop_id` bigint(20) NOT NULL,
  `user_id` bigint(20) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- 傾印資料表的資料 `reviews`
--

INSERT INTO `reviews` (`id`, `content`, `created_at`, `rating`, `reply_count`, `updated_at`, `parent_review_id`, `shop_id`, `user_id`) VALUES
(7, '5', '2025-04-13 17:32:31.000000', 4, 1, '2025-04-13 19:42:21.000000', NULL, 5, 3),
(9, '159', '2025-04-13 19:42:21.000000', NULL, 0, '2025-04-13 19:42:21.000000', 7, 5, 8);

-- --------------------------------------------------------

--
-- 資料表結構 `review_media`
--

CREATE TABLE `review_media` (
  `id` bigint(20) NOT NULL,
  `type` varchar(20) DEFAULT NULL,
  `url` varchar(1024) NOT NULL,
  `review_id` bigint(20) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- 傾印資料表的資料 `review_media`
--

INSERT INTO `review_media` (`id`, `type`, `url`, `review_id`) VALUES
(1, 'IMAGE', 'reviews/7/aaca1fd9-d9c0-4bf5-8659-5f538d1955c2.jpg', 7);

-- --------------------------------------------------------

--
-- 資料表結構 `shops`
--

CREATE TABLE `shops` (
  `id` bigint(20) NOT NULL,
  `address` varchar(512) NOT NULL,
  `average_rating` decimal(3,2) DEFAULT 0.00,
  `created_at` datetime(6) DEFAULT NULL,
  `description` text DEFAULT NULL,
  `latitude` decimal(10,7) DEFAULT NULL,
  `longitude` decimal(10,7) DEFAULT NULL,
  `name` varchar(255) NOT NULL,
  `opening_hours` text DEFAULT NULL,
  `phone` varchar(255) DEFAULT NULL,
  `review_count` int(11) DEFAULT 0,
  `updated_at` datetime(6) DEFAULT NULL,
  `weighted_rating` decimal(10,7) DEFAULT 0.0000000,
  `owner_id` bigint(20) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- 傾印資料表的資料 `shops`
--

INSERT INTO `shops` (`id`, `address`, `average_rating`, `created_at`, `description`, `latitude`, `longitude`, `name`, `opening_hours`, `phone`, `review_count`, `updated_at`, `weighted_rating`, `owner_id`) VALUES
(5, '新北市淡水區英專路21巷11號', 4.00, '2025-04-13 06:14:03.000000', '', 25.1696500, 121.4451181, '123', '', '02 XXXXXXXX', 1, '2025-04-13 19:37:32.000000', 3.7500000, 8),
(6, '新北市板橋區自由路33號', 0.00, '2025-04-13 11:32:07.000000', '123', 25.0230254, 121.4654169, 'aa4', '11:30~14:00、17:00~21:00(週一公休)', '不提供', 0, '2025-04-13 13:29:47.000000', 0.0000000, 9);

-- --------------------------------------------------------

--
-- 資料表結構 `shop_media`
--

CREATE TABLE `shop_media` (
  `id` bigint(20) NOT NULL,
  `display_order` int(11) DEFAULT NULL,
  `type` varchar(20) DEFAULT NULL,
  `url` varchar(1024) NOT NULL,
  `shop_id` bigint(20) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- 傾印資料表的資料 `shop_media`
--

INSERT INTO `shop_media` (`id`, `display_order`, `type`, `url`, `shop_id`) VALUES
(7, 0, 'image', 'shops/5/a066e87d-bb23-4f4e-9f37-68188298afea.jpg', 5);

-- --------------------------------------------------------

--
-- 資料表結構 `users`
--

CREATE TABLE `users` (
  `id` bigint(20) NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `email` varchar(255) NOT NULL,
  `enabled` tinyint(1) NOT NULL DEFAULT 1,
  `password` varchar(255) NOT NULL,
  `role` enum('ROLE_USER','ROLE_SHOP_OWNER','ROLE_ADMIN') NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `username` varchar(255) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- 傾印資料表的資料 `users`
--

INSERT INTO `users` (`id`, `created_at`, `email`, `enabled`, `password`, `role`, `updated_at`, `username`) VALUES
(2, '2025-04-13 00:43:00.000000', 'admin@ramenmap.local', 1, '$2a$10$uGrk1JJd8zrcdytWiWXUyerLoQBlORa2vKm.RE4Hzndu9tMEC3Tre', 'ROLE_ADMIN', '2025-04-13 00:43:00.000000', 'root'),
(3, '2025-04-13 03:28:08.000000', '111534201@stu.ukn.edu.tw', 1, '$2a$10$BL2ZuDkwA/ysw/.zF/SPSumdEy/jyLgmoQXxBd7MI1EYpLQigVg4m', 'ROLE_USER', '2025-04-13 03:28:08.000000', 'shinomiya'),
(8, '2025-04-13 06:14:03.000000', '123@gmail.com', 1, '$2a$10$hWf.ChHk0naD7mM/wzcgdOhr6oPCxBo6Ozi.yGHcL4xblFBkTGN2S', 'ROLE_SHOP_OWNER', '2025-04-13 06:14:03.000000', '123'),
(9, '2025-04-13 11:32:06.000000', 'abc@gmail.com', 1, '$2a$10$OV13Ik.abdoLEgPfdyXKkeqlwzeYUwbVi7c0YFqa8esBYNAAz3k/u', 'ROLE_SHOP_OWNER', '2025-04-13 11:32:06.000000', 'abc');

--
-- 已傾印資料表的索引
--

--
-- 資料表索引 `events`
--
ALTER TABLE `events`
  ADD PRIMARY KEY (`id`),
  ADD KEY `FKkcrgyg09j08hne5aajsrs8lq1` (`shop_id`);

--
-- 資料表索引 `event_media`
--
ALTER TABLE `event_media`
  ADD PRIMARY KEY (`id`),
  ADD KEY `FK_event_media_event` (`event_id`);

--
-- 資料表索引 `reviews`
--
ALTER TABLE `reviews`
  ADD PRIMARY KEY (`id`),
  ADD KEY `FKjvbonqfdvou0pln3uxbkvtxks` (`parent_review_id`),
  ADD KEY `FK3a0c998ccabg95h3c160yoq11` (`shop_id`),
  ADD KEY `FKcgy7qjc1r99dp117y9en6lxye` (`user_id`);

--
-- 資料表索引 `review_media`
--
ALTER TABLE `review_media`
  ADD PRIMARY KEY (`id`),
  ADD KEY `FK5eeii8rke42oss6yq63k2hrte` (`review_id`);

--
-- 資料表索引 `shops`
--
ALTER TABLE `shops`
  ADD PRIMARY KEY (`id`),
  ADD KEY `FKrduswa89ayj0poad3l70nag19` (`owner_id`);

--
-- 資料表索引 `shop_media`
--
ALTER TABLE `shop_media`
  ADD PRIMARY KEY (`id`),
  ADD KEY `FKa1ixg91e3nd52w1owv396jkbp` (`shop_id`);

--
-- 資料表索引 `users`
--
ALTER TABLE `users`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `UKr43af9ap4edm43mmtq01oddj6` (`username`),
  ADD UNIQUE KEY `UK6dotkott2kjsp8vw4d0m25fb7` (`email`);

--
-- 在傾印的資料表使用自動遞增(AUTO_INCREMENT)
--

--
-- 使用資料表自動遞增(AUTO_INCREMENT) `events`
--
ALTER TABLE `events`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT;

--
-- 使用資料表自動遞增(AUTO_INCREMENT) `event_media`
--
ALTER TABLE `event_media`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT;

--
-- 使用資料表自動遞增(AUTO_INCREMENT) `reviews`
--
ALTER TABLE `reviews`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=10;

--
-- 使用資料表自動遞增(AUTO_INCREMENT) `review_media`
--
ALTER TABLE `review_media`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=2;

--
-- 使用資料表自動遞增(AUTO_INCREMENT) `shops`
--
ALTER TABLE `shops`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=7;

--
-- 使用資料表自動遞增(AUTO_INCREMENT) `shop_media`
--
ALTER TABLE `shop_media`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=8;

--
-- 使用資料表自動遞增(AUTO_INCREMENT) `users`
--
ALTER TABLE `users`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=11;

--
-- 已傾印資料表的限制式
--

--
-- 資料表的限制式 `events`
--
ALTER TABLE `events`
  ADD CONSTRAINT `FKkcrgyg09j08hne5aajsrs8lq1` FOREIGN KEY (`shop_id`) REFERENCES `shops` (`id`);

--
-- 資料表的限制式 `event_media`
--
ALTER TABLE `event_media`
  ADD CONSTRAINT `FK_event_media_event` FOREIGN KEY (`event_id`) REFERENCES `events` (`id`) ON DELETE CASCADE;

--
-- 資料表的限制式 `reviews`
--
ALTER TABLE `reviews`
  ADD CONSTRAINT `FK3a0c998ccabg95h3c160yoq11` FOREIGN KEY (`shop_id`) REFERENCES `shops` (`id`),
  ADD CONSTRAINT `FKcgy7qjc1r99dp117y9en6lxye` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
  ADD CONSTRAINT `FKjvbonqfdvou0pln3uxbkvtxks` FOREIGN KEY (`parent_review_id`) REFERENCES `reviews` (`id`);

--
-- 資料表的限制式 `review_media`
--
ALTER TABLE `review_media`
  ADD CONSTRAINT `FK5eeii8rke42oss6yq63k2hrte` FOREIGN KEY (`review_id`) REFERENCES `reviews` (`id`);

--
-- 資料表的限制式 `shops`
--
ALTER TABLE `shops`
  ADD CONSTRAINT `FKrduswa89ayj0poad3l70nag19` FOREIGN KEY (`owner_id`) REFERENCES `users` (`id`);

--
-- 資料表的限制式 `shop_media`
--
ALTER TABLE `shop_media`
  ADD CONSTRAINT `FKa1ixg91e3nd52w1owv396jkbp` FOREIGN KEY (`shop_id`) REFERENCES `shops` (`id`);
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
