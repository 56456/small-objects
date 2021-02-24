package com.ggq.monitortoexl.common;



import ch.ethz.ssh2.Connection;
import ch.ethz.ssh2.Session;
import ch.ethz.ssh2.StreamGobbler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class OnlineServerTools {
	private String host;
	private String user;
	private String pwd;
	private String port;
	private Connection conn = null;
	private boolean isAuthenticated = false;
	
	public boolean init(String host, String user, String pwd, String port) {
		this.host = host;
		this.user = user;
		this.pwd = pwd;
		this.port = port;
		
		try {
			conn = new Connection(host, Integer.parseInt(port));
			conn.connect();
			isAuthenticated = conn.authenticateWithPassword(user, pwd);
		} catch (IOException e) {
			e.printStackTrace(System.err);
		}
		return isAuthenticated;
	}
	
	
	public void close() {
		if(conn!=null) {
			conn.close();
		}
	}
	
	
	public String execCommand(String command) throws Exception {
		if(!isAuthenticated) {
			throw new Exception("OnlineServerTools没有初始化");
		}
		//开启一个Session
		Session sess = conn.openSession();
        //执行具体命令
		sess.execCommand(command);
        //获取返回输出
		InputStream stdout = new StreamGobbler(sess.getStdout());
        //返回错误输出
		InputStream stderr = new StreamGobbler(sess.getStderr());
		BufferedReader stdoutReader = new BufferedReader(
				new InputStreamReader(stdout, "UTF-8"));
		BufferedReader stderrReader = new BufferedReader(
				new InputStreamReader(stderr, "UTF-8"));
		
		StringBuffer stout = new StringBuffer();
//		System.out.println("Here is the output from stdout:");
		while (true) {
			String line = stdoutReader.readLine();
			if (line == null)
				break;
//			System.out.println(line);
			stout.append(line+"\r\n");
		}

//		System.out.println("Here is the output from stderr:");
		while (true) {
			String line = stderrReader.readLine();
			if (line == null)
				break;
//			System.out.println(line);
		}
        //关闭Session
		stdoutReader.close();
		stderrReader.close();
		sess.close();
		return stout.toString();
	}
	
	public static void main(String[] args) {
		OnlineServerTools ost = new OnlineServerTools();

		System.out.println(ost.init("123.57.56.4", "root", "Woshi56456!", "22"));
		try {
			System.out.println(ost.execCommand("cat /proc/meminfo | grep MemTotal"));
			ost.close();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
//		System.out.println(ost.init("39.107.61.155", "root", "Hljhx!@$2020", 22));
	}
}
