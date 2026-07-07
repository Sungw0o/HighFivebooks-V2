CREATE DATABASE IF NOT EXISTS highfive_order
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

CREATE DATABASE IF NOT EXISTS highfive_book
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

CREATE DATABASE IF NOT EXISTS highfive_member
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

CREATE DATABASE IF NOT EXISTS highfive_coupon
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

CREATE DATABASE IF NOT EXISTS highfive_payment
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

CREATE USER IF NOT EXISTS 'order_user'@'%' IDENTIFIED BY 'order_pass';
CREATE USER IF NOT EXISTS 'book_user'@'%' IDENTIFIED BY 'book_pass';
CREATE USER IF NOT EXISTS 'member_user'@'%' IDENTIFIED BY 'member_pass';
CREATE USER IF NOT EXISTS 'coupon_user'@'%' IDENTIFIED BY 'coupon_pass';
CREATE USER IF NOT EXISTS 'payment_user'@'%' IDENTIFIED BY 'payment_pass';

GRANT ALL PRIVILEGES ON highfive_order.* TO 'order_user'@'%';
GRANT ALL PRIVILEGES ON highfive_book.* TO 'book_user'@'%';
GRANT ALL PRIVILEGES ON highfive_member.* TO 'member_user'@'%';
GRANT ALL PRIVILEGES ON highfive_coupon.* TO 'coupon_user'@'%';
GRANT ALL PRIVILEGES ON highfive_payment.* TO 'payment_user'@'%';

FLUSH PRIVILEGES;
