package info.coderman.dhtcrawler.constant;
/**
 * 
 * @author yuezixin
 *
 */
public interface RedisConstant {
	
	
	String ANNOUNCE_HASH_PRE="announce:hash:";
	
	int ANNOUNCE_HASH_EXPIRE=60*60*2;
	
	/**
	 * 等待入库队列 list
	 */
	String TEMP_HASH_QUEUE= "tempHashQueue";

	/**
	 * 磁力链数据结构  hash
	 */
	String HASH_MODE="hashModel";
	
	/**
	 * 已经入库的hash set
	 */
	String SAVED_HASHES="savedHashes";
}
