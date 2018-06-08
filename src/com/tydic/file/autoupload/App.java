package com.tydic.file.autoupload;

import com.tydic.file.autoupload.runnable.UploadThread;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.*;

/**
 * @author: jhs
 * @desc:
 * @date: Create in 2018/4/17  8:48
 */
public class App {
	private static Log log = LogFactory.getLog(App.class);
	
	private final static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
	private static final int POOL_MULTI = 4;
	private static ExecutorService pool;
	private static Set<WatchService> cachedWatchServiceSet = new CopyOnWriteArraySet<>(); //缓存记录，监听列表
	
	private static String protocol;
	private static String host;
	private static int port;
	private static String urlPath;
	public static String UPLOAD_URL;
	
	private static String folder;
	private static String fileType;
	
	/**
	 * 初始化 启动参数
	 *
	 * @param args
	 */
	private static void init(String[] args) throws MalformedURLException {
		if (args == null || args.length != 6) {
			log.error("启动参数无效,请输入启动参数：[protocol,host,port,urlPath,folder,fileType]");
			System.exit(1);
		}
		protocol = args[0];
		host = args[1];
		port = Integer.parseInt(args[2]);
		urlPath = args[3];
		folder = args[4];
		fileType = args[5];
		StringBuilder sb = new StringBuilder(protocol).append("://").append(host).append(":").append(port).append(urlPath);
		UPLOAD_URL = sb.toString();
		log.info("文件上传url为: " + UPLOAD_URL + ",监控root目录: " + folder + "\n");
	}
	
	/**
	 * 监听--文件变化
	 *
	 * @throws IOException
	 */
	private static void startWatch() throws IOException {
		Path dirPath = Paths.get(folder, sdf.format(new Date()));
		if (!Files.exists(dirPath)) {
			Files.createDirectories(dirPath);
		}
		//处理启动前，已经存在的文件
		Files.list(dirPath).filter((Path filePath) -> {
			return filePath.getFileName().toString().toLowerCase().endsWith(fileType);
		}).forEach((Path item) -> {
			pool.execute(new UploadThread(item));
		});
		
		//监听-新增的文件
		try (WatchService watchService = FileSystems.getDefault().newWatchService()) {
			cachedWatchServiceSet.add(watchService); //新增-最新的监听
			
			//给path路径加上文件观察服务
			dirPath.register(watchService, StandardWatchEventKinds.ENTRY_CREATE);
			
			log.info("开始监听:" + dirPath);
			while (true) {
				final WatchKey key = watchService.take();
				key.pollEvents().stream().forEach((WatchEvent watchEvent) ->{
					final WatchEvent.Kind<?> kind = watchEvent.kind();
					//创建事件
					if (kind == StandardWatchEventKinds.ENTRY_CREATE) {
						WatchEvent<Path> watchEventPath = (WatchEvent<Path>) watchEvent;
						String filename = watchEventPath.context().getFileName().toString();
						if (filename.toLowerCase().endsWith(fileType)) {
							pool.execute(new UploadThread(dirPath.resolve(filename)));
						}
					}
				});
				boolean valid = key.reset();
				if (!valid) {
					break;
				}
			}
		} catch (IOException | InterruptedException ex) {
			log.error(ex);
		}
	}
	
	/**
	 * 获取今天还剩下多少秒
	 * @return
	 */
	private static long getMiao() {
		long nowSeconds = LocalDateTime.now().toEpochSecond(ZoneOffset.of("+8"));
		long tomorrow000Seconds = LocalDateTime.of(LocalDate.now().plusDays(1), LocalTime.MIN).toEpochSecond(ZoneOffset.of("+8"));
		long delay = tomorrow000Seconds - nowSeconds + 5; //5秒缓存
		log.info("delay="+delay+"秒");
		return delay;
	}
	
	
	public static void main(String[] args) throws IOException {
		//初始化--获取的args参数
		init(args);
		
		//初始化线程池
		pool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * POOL_MULTI);
		
		//启动main方法时，立即启动监听
		new Thread(new StartWatchThread()).start();
		
		//监听日期变化 --- 为了防止，长期不关机，每天执行一次,监听yyyy-MM-dd 目录
		ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
		//延迟delay后执行 ，之后每隔period执行一次,timeunit时间单位
		scheduledExecutorService.scheduleAtFixedRate(new StartWatchThread(), getMiao(), 24 * 60 * 60, TimeUnit.SECONDS);
		
		/**注册关闭钩子*/
		Runtime.getRuntime().addShutdownHook(new Thread(()->{
			log.info("关闭勾子函数,执行...");
			cachedWatchServiceSet.stream().forEach((WatchService item) -> {
				try {
					item.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			});
		}));
	}
	
	//监听Thread
	private static class StartWatchThread implements Runnable {
		
		@Override
		public void run() {
			try {
				startWatch();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

}
