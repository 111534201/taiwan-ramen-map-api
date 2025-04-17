-- phpMyAdmin SQL Dump
-- version 5.2.1
-- https://www.phpmyadmin.net/
--
-- 主機： 127.0.0.1
-- 產生時間： 2025-04-17 02:56:47
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
-- 資料庫： `taiwan_ramen_map`
--

-- --------------------------------------------------------

--
-- 資料表結構 `events`
--

CREATE TABLE `events` (
  `id` bigint(20) NOT NULL,
  `shop_id` bigint(20) NOT NULL,
  `content` text NOT NULL,
  `created_at` datetime(6) DEFAULT current_timestamp(6),
  `updated_at` datetime(6) DEFAULT current_timestamp(6) ON UPDATE current_timestamp(6)
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
  `updated_at` datetime(6) DEFAULT NULL,
  `shop_id` bigint(20) NOT NULL,
  `user_id` bigint(20) NOT NULL,
  `parent_review_id` bigint(20) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- 傾印資料表的資料 `reviews`
--

INSERT INTO `reviews` (`id`, `content`, `created_at`, `rating`, `updated_at`, `shop_id`, `user_id`, `parent_review_id`) VALUES
(2, '祥子', '2025-04-12 00:52:38.000000', 5, '2025-04-12 00:52:38.000000', 1, 1, NULL),
(3, '', '2025-04-12 01:12:00.000000', 5, '2025-04-12 01:12:00.000000', 1, 1, NULL),
(4, '???', '2025-04-12 05:08:05.000000', NULL, '2025-04-12 05:08:05.000000', 1, 7, 3),
(5, '1', '2025-04-12 07:00:09.000000', 4, '2025-04-12 07:00:09.000000', 2, 1, NULL);

-- --------------------------------------------------------

--
-- 資料表結構 `review_media`
--

CREATE TABLE `review_media` (
  `id` bigint(20) NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `file_path_or_url` varchar(1024) NOT NULL,
  `media_type` varchar(100) DEFAULT NULL,
  `original_filename` varchar(255) DEFAULT NULL,
  `review_id` bigint(20) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- 傾印資料表的資料 `review_media`
--

INSERT INTO `review_media` (`id`, `created_at`, `file_path_or_url`, `media_type`, `original_filename`, `review_id`) VALUES
(2, '2025-04-12 00:52:38.000000', '990a8ab3-90f6-4006-8ae0-f4db15e48040.jpeg', 'image/jpeg', 'jrw6LJp.jpeg', 2),
(3, '2025-04-12 01:12:00.000000', 'dbe44486-88e9-419f-8d2b-ae6c3ce1030c.jpg', 'image/jpeg', '1223.jpg', 3),
(4, '2025-04-12 05:08:05.000000', 'b5c9cdef-d5f7-4289-8b9c-2ac5686e8964.jpg', 'image/jpeg', 'OIP.jpg', 4);

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
  `owner_id` bigint(20) DEFAULT NULL,
  `weighted_rating` decimal(10,7) DEFAULT 0.0000000
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- 傾印資料表的資料 `shops`
--

INSERT INTO `shops` (`id`, `address`, `average_rating`, `created_at`, `description`, `latitude`, `longitude`, `name`, `opening_hours`, `phone`, `review_count`, `updated_at`, `owner_id`, `weighted_rating`) VALUES
(1, '242新北市新莊區龍安路236號', 5.00, '2025-04-11 23:21:53.000000', '為甚麼要演奏春日影!!!', 25.0197168, 121.4206370, '邦邦拉麵館', '星期一 12:00–00:00\n星期二 休息\n星期三 12:00–00:00\n星期四 12:00–00:00\n星期五 12:00–00:00\n星期六 12:00–00:00\n星期日 12:00–00:00', '02 XXXXX XXXXX', 2, '2025-04-12 01:12:00.000000', 7, 0.0000000),
(2, '臺中市西屯區河南路三段120號3樓', 4.00, '2025-04-12 06:57:13.000000', 'OuO\n', 24.1642474, 120.6381239, '忒鮮拉麵', '星期一~星期四11:30-21:00\n星期五11:30-22:00\n星期六11:00-22:00\n星期日11:00-21:00', '04-36062511', 1, '2025-04-12 07:00:09.000000', 7, 3.9000000);

-- --------------------------------------------------------

--
-- 資料表結構 `shop_media`
--

CREATE TABLE `shop_media` (
  `id` bigint(20) NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `file_path_or_url` varchar(1024) NOT NULL,
  `media_type` varchar(100) DEFAULT NULL,
  `original_filename` varchar(255) DEFAULT NULL,
  `shop_id` bigint(20) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- 傾印資料表的資料 `shop_media`
--

INSERT INTO `shop_media` (`id`, `created_at`, `file_path_or_url`, `media_type`, `original_filename`, `shop_id`) VALUES
(3, '2025-04-12 00:27:36.000000', 'e73a10d5-661c-4f1a-8d57-576a490da839.png', 'image/png', 'a1cdde9690574771a20e30366b177b88.png', 1),
(4, '2025-04-12 06:57:13.000000', '2f629f9b-1f97-47ad-a272-2d7bed222ec4.jpg', 'image/jpeg', '20231210092259_0.jpg', 2);

-- --------------------------------------------------------

--
-- 資料表結構 `users`
--

CREATE TABLE `users` (
  `id` bigint(20) NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `email` varchar(255) NOT NULL,
  `password` varchar(255) NOT NULL,
  `role` enum('ROLE_ADMIN','ROLE_SHOP_OWNER','ROLE_USER') NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `username` varchar(255) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- 傾印資料表的資料 `users`
--

INSERT INTO `users` (`id`, `created_at`, `email`, `password`, `role`, `updated_at`, `username`) VALUES
(1, '2025-04-10 23:14:50.000000', 'chocosensei214@gmail.com', '$2a$10$XZpdB.djvzl6/02fOog6Zu0EcUFyACByxANgilTTw95F2WP2m2qvC', 'ROLE_USER', '2025-04-10 23:14:50.000000', 'shinomiya'),
(4, '2025-04-11 22:50:13.000000', 'admin@example.com', '$2a$10$jJ9klhD253WAwxngfYv4SOk8ayaOzBBgOmP/2SkKGW64m5A04HMtO', 'ROLE_ADMIN', '2025-04-11 22:50:13.000000', 'root'),
(7, '2025-04-11 23:21:53.000000', 'abc@gmail.com', '$2a$10$VYZQ0GkcDKhUEGuAj/30vutdOPoabpcIYZ3ZT3X3EK44/NH/mFrUu', 'ROLE_SHOP_OWNER', '2025-04-11 23:21:53.000000', 'abc');

--
-- 已傾印資料表的索引
--

--
-- 資料表索引 `events`
--
ALTER TABLE `events`
  ADD PRIMARY KEY (`id`),
  ADD KEY `FK_events_shop` (`shop_id`);

--
-- 資料表索引 `reviews`
--
ALTER TABLE `reviews`
  ADD PRIMARY KEY (`id`),
  ADD KEY `FK3a0c998ccabg95h3c160yoq11` (`shop_id`),
  ADD KEY `FKcgy7qjc1r99dp117y9en6lxye` (`user_id`),
  ADD KEY `FKjvbonqfdvou0pln3uxbkvtxks` (`parent_review_id`);

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
-- 使用資料表自動遞增(AUTO_INCREMENT) `reviews`
--
ALTER TABLE `reviews`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=6;

--
-- 使用資料表自動遞增(AUTO_INCREMENT) `review_media`
--
ALTER TABLE `review_media`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=5;

--
-- 使用資料表自動遞增(AUTO_INCREMENT) `shops`
--
ALTER TABLE `shops`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=3;

--
-- 使用資料表自動遞增(AUTO_INCREMENT) `shop_media`
--
ALTER TABLE `shop_media`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=5;

--
-- 使用資料表自動遞增(AUTO_INCREMENT) `users`
--
ALTER TABLE `users`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=8;

--
-- 已傾印資料表的限制式
--

--
-- 資料表的限制式 `events`
--
ALTER TABLE `events`
  ADD CONSTRAINT `FK_events_shop` FOREIGN KEY (`shop_id`) REFERENCES `shops` (`id`) ON DELETE CASCADE;

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
