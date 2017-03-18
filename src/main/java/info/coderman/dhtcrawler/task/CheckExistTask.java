package info.coderman.dhtcrawler.task;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;

import org.apache.log4j.Logger;

import info.coderman.dhtcrawler.constant.RedisConstant;
import info.coderman.dhtcrawler.db.RedisPool;
import info.coderman.dhtcrawler.main.Main;
import info.coderman.dhtcrawler.structure.DownloadPeer;
import info.coderman.dhtcrawler.util.ByteUtil;
import redis.clients.jedis.Jedis;

public class CheckExistTask extends Thread {
	private static final Logger LOG = Logger.getLogger(CheckExistTask.class);

	private DownloadPeer peer;
	private ExecutorService metadataDwonloadThreadPool;
	private Integer max_thread;
	private BlockingQueue<DownloadPeer> hashQueue;
	private List<DownloadPeer> list = new ArrayList<>();
	
	public CheckExistTask(BlockingQueue<DownloadPeer> hashQueue, 
			ExecutorService metadataDwonloadThreadPool, Integer mAX_THREAD2) {
		this.hashQueue = hashQueue;
		this.metadataDwonloadThreadPool = metadataDwonloadThreadPool;
		this.max_thread = mAX_THREAD2;
	}


	@Override
	public void run() {
		Jedis jedis = RedisPool.getJedis();
		while (!isInterrupted()) {
			try {
				peer = hashQueue.take();
				if(jedis.sismember(RedisConstant.SAVED_HASHES,ByteUtil.byteArrayToHex(peer.getInfo_hash()))){
					continue;
				}
				list.add(peer);
				if (list.size() >= 10) {
					for (int i = 0; i < list.size(); i++) {
						if (Main.count.get() < max_thread) {
							metadataDwonloadThreadPool.execute(new WireMetadataDownloadTask(list.get(i)));
							Main.count.incrementAndGet();
						}
					}
					list.clear();
				}
			
			} catch (Exception e) {
				LOG.error("CheckExisTask is error:"+e.getMessage(),e);
			} 
		}
	
	}
}
