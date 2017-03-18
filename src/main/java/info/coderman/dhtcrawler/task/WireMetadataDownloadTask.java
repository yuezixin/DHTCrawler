package info.coderman.dhtcrawler.task;

import java.net.InetSocketAddress;
import java.sql.Timestamp;
import java.util.Date;

import info.coderman.dhtcrawler.handler.AnnouncePeerInfoHashWireHandler;
import info.coderman.dhtcrawler.listener.OnMetadataListener;
import info.coderman.dhtcrawler.main.Main;
import info.coderman.dhtcrawler.structure.DownloadPeer;
import info.coderman.dhtcrawler.structure.Torrent;
import info.coderman.dhtcrawler.structure.TorrentFile;
import info.coderman.dhtcrawler.util.JsonUtils;

public class WireMetadataDownloadTask implements Runnable {
	
	private DownloadPeer peer;
	
	public WireMetadataDownloadTask(DownloadPeer peer) {
		this.peer = peer;
	}

	@Override
	public void run() {

			AnnouncePeerInfoHashWireHandler handler = new AnnouncePeerInfoHashWireHandler();
			initHandler(handler);
			try {
				handler.handler(new InetSocketAddress(peer.getIp(), peer.getPort()), peer.getInfo_hash());
			} catch (Exception e) {
				//jedis.del(ByteUtil.byteArrayToHex(peer.getInfo_hash()));
				//e.printStackTrace();
				//System.out.println(e.getMessage());
			} finally {
				//jedis.del(peer.getInfo_hash());
				Main.count.decrementAndGet();
				handler.release();
				handler = null;
				peer = null;
			}

	}
	
	private void initHandler(AnnouncePeerInfoHashWireHandler handler) {
	//private void initHandler() {
		handler.setOnMetadataListener(new OnMetadataListener() {
			@Override
			public void onMetadata(Torrent torrent) {
					//System.out.println("finished,dps size:" + dps.size());
					if (torrent == null || torrent.getInfo() == null)
						return;
 					//入库操作
					TorrentFile tFile=new TorrentFile();
					tFile.setInfoHash(torrent.getInfo_hash());
					tFile.setName(torrent.getInfo().getName());
					tFile.setSize(torrent.getInfo().getLength());
					tFile.setType(torrent.getType());
					tFile.setFindDate(new Date());
					tFile.setHot(1);
					tFile.setSubfiles(JsonUtils.toJSONString(torrent.getInfo().getFiles()));
					Main.torrentQueue.insert(tFile);
				 
			}
		});
	}
	
}
