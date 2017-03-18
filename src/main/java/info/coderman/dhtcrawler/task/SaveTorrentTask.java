package info.coderman.dhtcrawler.task;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import info.coderman.dhtcrawler.constant.RedisConstant;
import info.coderman.dhtcrawler.db.RedisPool;
import info.coderman.dhtcrawler.structure.MyQueue;
import info.coderman.dhtcrawler.structure.TorrentFile;
import info.coderman.dhtcrawler.util.JsonUtils;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;

public class SaveTorrentTask extends Thread {
	private static final Logger LOG = Logger.getLogger(SaveTorrentTask.class);

	private MyQueue<TorrentFile> torrentQueue;
 	
	public SaveTorrentTask(MyQueue<TorrentFile> torrentQueue) {
		this.torrentQueue = torrentQueue;
	}
	
	@Override
	public void run() {
		Jedis jedis = RedisPool.getJedis();
		//插入数据库 TODO
		while (!this.isInterrupted()) {
			try {
				sleep(20000);
 				List<TorrentFile> list = torrentQueue.getAll();
				if (list.size() > 0) {
					Map<String,String> data=new HashMap<>();
					Pipeline p = jedis.pipelined();
					for (TorrentFile torrent : list) {
						data.clear();
						data.put(torrent.getInfoHash(), JsonUtils.toJSONString(torrent));
						p.hmset(RedisConstant.HASH_MODE, data);
						p.lpush(RedisConstant.TEMP_HASH_QUEUE, torrent.getInfoHash());
 					}
					p.sync();
 					list.clear();
					list = null;
				}
			} catch (Exception e) {
				LOG.error("SaveTorrentTask is error:"+e.getMessage(),e);
			}
		}
	}
	
}
