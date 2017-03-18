# DHTCrawler 一个简单的DHT爬虫。
## DHT实现了BitTorrent DHT协议。
## 协议参考：
  1. http://blog.csdn.net/xxxxxx91116/article/details/7970815
  2. http://www.xuebuyuan.com/1287052.html 
  
## 使用技术
  Redis、Netty、torrent-utils、bee-encode

### 请在外网环境下运行 监听端口6882.

### 使用方法
  1. 安装java环境 jdk1.8、Maven;
  2. RedisPool.java 修改redis ip;
  3. mvn package 打jar包;
  4. java -jar XXX.jar 运行。
