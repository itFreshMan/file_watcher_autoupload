package com.tydic.file.autoupload;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * @author: jhs
 * @desc:防止多次启动同一程序
 * @date: Create in 2018/6/8  13:55
 */
public class ProcesSingtonle {
	private static Log log = LogFactory.getLog(ProcesSingtonle.class);
	private static final String PID_FILE_NAME = "pid";
	public static void main(String[] args) throws InterruptedException, IOException {
		System.out.println(System.getProperty("user.dir"));
		int processId = getProcessID();
		Thread.sleep(1000);
//		Runtime.getRuntime().exec("taskkill /F /PID "+processId);
		while(true){
			Thread.sleep(1000);
		}
	}
	
	/**
	 * 获取进程ID
	 * @return
	 */
	public static final int getProcessID() {
		RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
		System.out.println(runtimeMXBean.getName());
		return Integer.valueOf(runtimeMXBean.getName().split("@")[0]).intValue();
		
	}
	
	public static void readPid(String folder) throws IOException {
		Path path = Paths.get(folder, PID_FILE_NAME);
		File f = path.toFile();
		if(f.exists()) {
			List<String> pids = Files.readAllLines(path);
		}else{
			try {
				log.info("创建pid.....");
				f.createNewFile();
				log.info("创建pid成功.....");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * 列出所有正在运行的java程序
	 */
	public static void listAllProcesses(){
	
	}
}
