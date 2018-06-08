package com.tydic.file.autoupload;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @author: jhs
 * @desc:防止多次启动同一程序
 * @date: Create in 2018/6/8  13:55
 */
public class CmdSingletonExec {
	private static Log log = LogFactory.getLog(CmdSingletonExec.class);
	private static final String PID_FILE_NAME = "pid";
	private static final Integer PID;
	
	static {
		RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
		log.info("当前进程信息：" + runtimeMXBean.getName());
		PID = Integer.valueOf(runtimeMXBean.getName().split("@")[0]).intValue();
	}
	
	public static void main(String[] args) throws InterruptedException, IOException {
		execCmd("c:");
		while (true) {
			Thread.sleep(1000);
		}
	}
	
	/**
	 * 关闭当前进程
	 *
	 * @throws IOException
	 */
	public static void shutdownCmd(String folder) throws IOException {
		Runtime.getRuntime().exec("taskkill /F /PID " + PID);
	}
	
	/**
	 * 列出所有正在运行的java程序
	 */
	public static void execCmd(String folder) throws IOException {
		Path path = Paths.get(folder, PID_FILE_NAME);
		File f = path.toFile();
		if (!f.exists()) {
			try {
				log.info("创建pid文件.....");
				f.createNewFile();
				log.info("创建pid文件成功.....");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		RandomAccessFile out = new RandomAccessFile(f, "rw");
		FileChannel fc=out.getChannel();
		FileLock fl = fc.tryLock();
		if(fl != null && fl.isValid()) {
			log.info("程序启动成功,@PID:" + PID);
		}else{
			log.warn("已有进程正在执行,关闭当前进程:" + PID);
			Runtime.getRuntime().exec("taskkill /F /PID " + PID);
			return;
		}
	}

}
