ALTER TABLE `wangle`.`tweets`
CHANGE COLUMN `follow_retweets` `follow_retweets` BIT(3) NULL DEFAULT b'100' COMMENT '0 = crawled\n1 = follow\n2 = followed\n3 = uninteresting\n4 = unprocessed' ;
