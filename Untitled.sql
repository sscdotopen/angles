use wangle;

select * from tweets;

select * from website_labels;

SELECT DISTINCT t.explorer_id FROM tweets t LEFT JOIN explorers e ON t.explorer_id = e.id WHERE e.id IS NULL;

insert into tweets (id, explorer_id, json) values (123, 4711, '');

select * from tweets where id = 123;

SELECT id,retweet_count FROM tweets WHERE retweet_count>0 AND follow_retweets=0;

update tweets set parent_tweet=-1;

SELECT * FROM crawled_websites;

select count(*) from crawled_websites;

delete from crawled_websites where real_uri='http://www.journalistenpreise.de/?id=news_detail&newsid=434';

select count(DISTINCT real_uri) from crawled_websites;

select distinct real_uri from crawled_websites;

select * from crawled_websites;

SELECT count(*) FROM crawled_websites w JOIN tweets t on w.tweet_id = t.id WHERE  t.follow_retweets = 0 and t.retweet_count > 0;

select count(*) from tweets where follow_retweets = 0;

update tweets set follow_retweets = 3 where follow_retweets = 0;

SELECT w.* FROM crawled_websites w JOIN tweets t on w.tweet_id = t.id WHERE w.fetch_time >= '2015-01-30 13:10:12.196' AND t.follow_retweets = 0 AND t.retweet_count > 0 LIMIT 500 OFFSET 0;
SELECT w.* FROM crawled_websites w JOIN tweets t on w.tweet_id = t.id WHERE w.fetch_time >= '2015-01-30 13:10:12.196' AND t.follow_retweets = 0 AND t.retweet_count > 0 LIMIT 500 OFFSET 500;
SELECT w.* FROM crawled_websites w JOIN tweets t on w.tweet_id = t.id WHERE w.fetch_time >= '2015-01-30 13:10:12.196' AND t.follow_retweets = 0 AND t.retweet_count > 0 LIMIT 500 OFFSET 1000;
SELECT w.* FROM crawled_websites w JOIN tweets t on w.tweet_id = t.id WHERE w.fetch_time >= '2015-01-30 13:10:12.196' AND t.follow_retweets = 0 AND t.retweet_count > 0 LIMIT 500 OFFSET 1500;
SELECT w.* FROM crawled_websites w JOIN tweets t on w.tweet_id = t.id WHERE w.fetch_time >= '2015-01-30 13:10:12.196' AND t.follow_retweets = 0 AND t.retweet_count > 0 LIMIT 500 OFFSET 2000;
SELECT w.* FROM crawled_websites w JOIN tweets t on w.tweet_id = t.id WHERE w.fetch_time >= '2015-01-30 13:10:12.196' AND t.follow_retweets = 0 AND t.retweet_count > 0 LIMIT 500 OFFSET 2500;
SELECT w.* FROM crawled_websites w JOIN tweets t on w.tweet_id = t.id WHERE w.fetch_time >= '2015-01-30 13:10:12.196' AND t.follow_retweets = 0 AND t.retweet_count > 0 LIMIT 500 OFFSET 3000;
SELECT w.* FROM crawled_websites w JOIN tweets t on w.tweet_id = t.id WHERE w.fetch_time >= '2015-01-30 13:10:12.196' AND t.follow_retweets = 0 AND t.retweet_count > 0 LIMIT 500 OFFSET 3500;
SELECT w.* FROM crawled_websites w JOIN tweets t on w.tweet_id = t.id WHERE w.fetch_time >= '2015-01-30 13:10:12.196' AND t.follow_retweets = 0 AND t.retweet_count > 0 LIMIT 500 OFFSET 4000;
SELECT w.* FROM crawled_websites w JOIN tweets t on w.tweet_id = t.id WHERE w.fetch_time >= '2015-01-30 13:10:12.196' AND t.follow_retweets = 0 AND t.retweet_count > 0 LIMIT 500 OFFSET 4500;
SELECT w.* FROM crawled_websites w JOIN tweets t on w.tweet_id = t.id WHERE w.fetch_time >= '2015-01-30 13:10:12.196' AND t.follow_retweets = 0 AND t.retweet_count > 0 LIMIT 500 OFFSET 5000;
SELECT w.* FROM crawled_websites w JOIN tweets t on w.tweet_id = t.id WHERE w.fetch_time >= '2015-01-30 13:10:12.196' AND t.follow_retweets = 0 AND t.retweet_count > 0 LIMIT 500 OFFSET 5500;
SELECT w.* FROM crawled_websites w JOIN tweets t on w.tweet_id = t.id WHERE w.fetch_time >= '2015-01-30 13:10:12.196' AND t.follow_retweets = 0 AND t.retweet_count > 0 LIMIT 500 OFFSET 6000;
SELECT w.* FROM crawled_websites w JOIN tweets t on w.tweet_id = t.id WHERE w.fetch_time >= '2015-01-30 13:10:12.196' AND t.follow_retweets = 0 AND t.retweet_count > 0 LIMIT 500 OFFSET 6500;
SELECT w.* FROM crawled_websites w JOIN tweets t on w.tweet_id = t.id WHERE w.fetch_time >= '2015-01-30 13:10:12.196' AND t.follow_retweets = 0 AND t.retweet_count > 0 LIMIT 500 OFFSET 7000;
SELECT w.* FROM crawled_websites w JOIN tweets t on w.tweet_id = t.id WHERE w.fetch_time >= '2015-01-30 13:10:12.196' AND t.follow_retweets = 0 AND t.retweet_count > 0 LIMIT 500 OFFSET 7500;
SELECT w.* FROM crawled_websites w JOIN tweets t on w.tweet_id = t.id WHERE w.fetch_time >= '2015-01-30 13:10:12.196' AND t.follow_retweets = 0 AND t.retweet_count > 0 LIMIT 500 OFFSET 8000;
SELECT w.* FROM crawled_websites w JOIN tweets t on w.tweet_id = t.id WHERE w.fetch_time >= '2015-01-30 13:10:12.196' AND t.follow_retweets = 0 AND t.retweet_count > 0 LIMIT 500 OFFSET 8500;
SELECT w.* FROM crawled_websites w JOIN tweets t on w.tweet_id = t.id WHERE w.fetch_time >= '2015-01-30 13:10:12.196' AND t.follow_retweets = 0 AND t.retweet_count > 0 LIMIT 500 OFFSET 9000;
SELECT w.* FROM crawled_websites w JOIN tweets t on w.tweet_id = t.id WHERE w.fetch_time >= '2015-01-30 13:10:12.196' AND t.follow_retweets = 0 AND t.retweet_count > 0 LIMIT 500 OFFSET 9500;
SELECT w.* FROM crawled_websites w JOIN tweets t on w.tweet_id = t.id WHERE w.fetch_time >= '2015-01-30 13:10:12.196' AND t.follow_retweets = 0 AND t.retweet_count > 0 LIMIT 500 OFFSET 10000;
SELECT w.* FROM crawled_websites w JOIN tweets t on w.tweet_id = t.id WHERE w.fetch_time >= '2015-01-30 13:10:12.196' AND t.follow_retweets = 0 AND t.retweet_count > 0 LIMIT 500 OFFSET 10500;

select 1;