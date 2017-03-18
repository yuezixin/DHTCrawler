package info.coderman.dhtcrawler.main;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.log4j.Logger;

import info.coderman.dhtcrawler.constant.RedisConstant;
import info.coderman.dhtcrawler.db.RedisPool;
import info.coderman.dhtcrawler.listener.OnAnnouncePeerListener;
import info.coderman.dhtcrawler.listener.OnGetPeersListener;
import info.coderman.dhtcrawler.server.DHTServer;
import info.coderman.dhtcrawler.structure.DownloadPeer;
import info.coderman.dhtcrawler.structure.MyQueue;
import info.coderman.dhtcrawler.structure.TorrentFile;
import info.coderman.dhtcrawler.task.CheckExistTask;
import info.coderman.dhtcrawler.task.SaveTorrentTask;
import info.coderman.dhtcrawler.util.ByteUtil;
import redis.clients.jedis.Jedis;

public class Main extends Thread {
	private static final Logger LOG = Logger.getLogger(Main.class);

	public static Main me = new Main();
	
	public static AtomicLong count = new AtomicLong(0);
	public static AtomicLong update_count = new AtomicLong(0);
	public static AtomicLong success_count = new AtomicLong(0);
	public static MyQueue<TorrentFile> torrentQueue = new MyQueue<>();
	private List<Thread> threads = new ArrayList<>();
	private DHTServer server = null;
	private ExecutorService metadataDwonloadThreadPool;
 	
	@Override
	public void run() {
		//连接redis缓存服务器
		Jedis jedis = RedisPool.getJedis();
		if (jedis == null) {
			LOG.error("get jedis failed.");
			return;
		}
		
		final Long MAX_INFO_HASH = 50000L;
		
		metadataDwonloadThreadPool = Executors.newFixedThreadPool(400);

		BlockingQueue<DownloadPeer> hashQueue = new LinkedBlockingQueue<>();
		//启动检测info_hash在数据库是否存在的线程
		for (int i = 0; i < 5; i++) {
			Thread t = new CheckExistTask(hashQueue, metadataDwonloadThreadPool, 2400);
			threads.add(t);
			t.start();
		}
		
		SaveTorrentTask saveTorrentTask = new SaveTorrentTask(torrentQueue);
		threads.add(saveTorrentTask);
		saveTorrentTask.start();
		
		server = new DHTServer("0.0.0.0",6882, 2000);
		
		//配置get_peer请求监听器
		server.setOnGetPeersListener(new OnGetPeersListener() {
			
			@Override
			public void onGetPeers(InetSocketAddress address, byte[] info_hash) {
			}
		});
		
		//配置announce_peers请求监听器
		server.setOnAnnouncePeerListener(new OnAnnouncePeerListener() {
			
			@Override
			public void onAnnouncePeer(InetSocketAddress address, byte[] info_hash, int port) {
 				if (hashQueue.size() > MAX_INFO_HASH)
					return;
				String key=RedisConstant.ANNOUNCE_HASH_PRE+ByteUtil.byteArrayToHex(info_hash);
				if(jedis.get(key)==null){
					jedis.set(key,"0");
 					jedis.expire(key, RedisConstant.ANNOUNCE_HASH_EXPIRE);
 					try {
						hashQueue.put(new DownloadPeer(address.getHostString(), port, info_hash));
					} catch (InterruptedException e) {
						LOG.error(e.getMessage(),e);
					}
				}
			}
		});
		server.start();
	}
	
	public void stopAll() {
		metadataDwonloadThreadPool.shutdown();
 		for (Thread t : threads) {
			t.interrupt();
		}
		if (server != null)
			server.stopAll();
	}
	public static void main(String[] args) {
		me.start();
		LOG.info("DHTCrawler start!");
	}
}
