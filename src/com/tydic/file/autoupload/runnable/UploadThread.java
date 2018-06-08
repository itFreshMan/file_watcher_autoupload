package com.tydic.file.autoupload.runnable;

import com.tydic.file.autoupload.App;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @author: jhs
 * @desc:
 * @date: Create in 2018/4/17  9:49
 */
public class UploadThread implements Runnable {
	private Log log = LogFactory.getLog(this.getClass());
	private Path path;
	
	public UploadThread(Path path) {
		this.path = path;
	}
	
	@Override
	public void run() {
		log.info("发送【" + path+"】.......");
		uploadFile(path);
	}
	
	private final static RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(10 * 1000).setSocketTimeout(60 * 1000).build();
	
	/**
	 * 将文件提交至文件服务器
	 *
	 * @param path 文件路径
	 * @return FileStatus 上传结果
	 */
	public void uploadFile(Path path) {
		//1:创建一个httpclient对象
		CloseableHttpClient httpclient = HttpClients.createDefault();
		CloseableHttpResponse response = null;
		try {
			HttpPost httpPost = new HttpPost(App.UPLOAD_URL);
			httpPost.setConfig(requestConfig);

			MultipartEntityBuilder mEntityBuilder = MultipartEntityBuilder.create();
			
			mEntityBuilder.addBinaryBody("file", path.toFile());
			httpPost.setEntity(mEntityBuilder.build());
			response = httpclient.execute(httpPost);
			
			int statusCode = response.getStatusLine().getStatusCode();
			if (statusCode == HttpStatus.SC_OK) {
				HttpEntity resEntity = response.getEntity();
				String result = EntityUtils.toString(resEntity);
				// 消耗掉response
				EntityUtils.consume(resEntity);
				
				log.info("上传文件【" + path + "】成功");
				//最终备份文件
				Path destPath = Paths.get(path.getParent().toString(), path.getFileName().toString() + ".aready_uploaded.bak");
				Files.move(path, destPath);
			}
		} catch (IOException e) {
			e.printStackTrace();
			log.error("上传文件【" + path + "】失败:" + e.getMessage());
		} finally {
			try {
				HttpClientUtils.closeQuietly(httpclient);
				HttpClientUtils.closeQuietly(response);
			} catch (Exception ignore) {
			}
		}
	}
}
