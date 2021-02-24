package com.ggq.monitortoexl.service;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFDataFormat;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;
import org.yaml.snakeyaml.Yaml;

import com.ggq.monitortoexl.common.OnlineServerTools;
import com.google.gson.Gson;

public class MonitorService {
	
	public void getServerInfoToExl() throws Exception {
		Gson gson = new Gson();
		OnlineServerTools ost = new OnlineServerTools();
		//创建工作薄对象
        HSSFWorkbook workbook=new HSSFWorkbook();//这里也可以设置sheet的Name
		HSSFSheet totalSheet = workbook.createSheet("统计");
		HSSFSheet highSheet = workbook.createSheet("最高值");
		int totalIndex = 0; // 统计用下标
		double cpuZYL = 0; // 最高CPU占用率
		double cpuIO = 0; // 最高CPU等待磁盘IO
		double memory = 0; // 最高内存已使用占比
        
        InputStream input = new FileInputStream(System.getProperty("user.dir") + java.io.File.separator + "config.yml");
    	Yaml yaml = new Yaml();
    	Map<String, List<Map<String, String>>> servers = (Map<String, List<Map<String, String>>>) yaml.load(input);
    	List<String> header = new ArrayList<String>();
    	header.add("ip");
    	
    	// 设置单元格格式
    	HSSFCellStyle textStyle = workbook.createCellStyle();
    	HSSFDataFormat format = workbook.createDataFormat();
    	textStyle.setDataFormat(format.getFormat("@"));
    	
    	//创建工作表对象
        HSSFSheet sheet = workbook.createSheet("sheet1");
        Row sheethead = sheet.createRow(0);
        int i = 1;
    	for (Map<String, String> server : servers.get("servers")) {
    		String host = server.get("ip");
    		String user = server.get("user");
    		String pwd = server.get("pwd");
    		String port = server.get("port");
    		String command = server.get("command");
    		
    		String serverInfoJson=null;
    		System.out.println("==================>IP:"+host+"<==================");

    		//创建工作表的行
    		HSSFRow row = sheet.createRow(i++);//设置第一行，从1开始
    		row.setHeight((short)(35*20));
        	row.createCell(0).setCellValue(host);//第一行第一列
        	if(ost.init(host, user, pwd, port)) {
    			serverInfoJson = ost.execCommand(command);
    			System.out.println(serverInfoJson);
    		}else {
    			Cell cell= row.createCell(1);//第一行第indexHeader列
            	cell.setCellValue(host+":服务器初始化失败");
            	cell.setCellStyle(textStyle);
            	System.out.println(host+":服务器初始化失败");
            	continue;
    		}
        	ArrayList<Map<String, String>> serverInfos = new ArrayList<Map<String, String>>();
    		serverInfos = gson.fromJson(serverInfoJson, serverInfos.getClass()); 
    		ost.close();
            for (Map<String, String> serverInfo : serverInfos) {
            	String name = serverInfo.get("name");
            	String value = serverInfo.get("value");
            	int indexHeader = header.indexOf(name);
            	if(indexHeader<0) {
            		indexHeader = header.size();
            		header.add(name);
            	}
            	Cell cell= row.createCell(indexHeader);//第一行第indexHeader列
            	cell.setCellValue(value);
            	cell.setCellStyle(textStyle);
            	
				if ("CPU占用率".equals(serverInfo.get("name"))) {
					String cpuvalue = serverInfo.get("value");
					int index = cpuvalue.indexOf("%");
					cpuvalue = cpuvalue.substring(0, index);
					double currentCpu = Double.valueOf(cpuvalue);
					if (currentCpu >= 60) {
						HSSFRow highCpuRow = totalSheet.createRow(totalIndex++);
						highCpuRow.createCell(0).setCellValue("高CPU占用率[" + host + "]");
						highCpuRow.createCell(1).setCellValue(currentCpu + "%");
					}
					if (currentCpu > cpuZYL) {
						cpuZYL = currentCpu;
					}
				}
				if ("CPU等待磁盘io".equals(serverInfo.get("name"))) {
					String cpvalue = serverInfo.get("value");
					int index = cpvalue.indexOf("%");
					cpvalue = cpvalue.substring(0, index);
					double currentIo = Double.valueOf(cpvalue);
					if (currentIo >= 30) {
						HSSFRow highIoRow = totalSheet.createRow(totalIndex++);
						highIoRow.createCell(0).setCellValue("高CPU等待磁盘io[" + host + "]");
						highIoRow.createCell(1).setCellValue(currentIo + "%");
					}
					if (currentIo > cpuIO) {
						cpuIO = currentIo;
					}
				}
				if ("内存已使用占比".equals(serverInfo.get("name"))) {
					String ncvalue = serverInfo.get("value");
					int index = ncvalue.indexOf("%");
					ncvalue = ncvalue.substring(0, index);
					double currentMemory = Double.valueOf(ncvalue);
					if (currentMemory >= 60) {
						HSSFRow memoryRow = totalSheet.createRow(totalIndex++);
						memoryRow.createCell(0).setCellValue("高内存已使用占比[" + host + "]");
						memoryRow.createCell(1).setCellValue(currentMemory + "%");
					}
					if (currentMemory > memory) {
						memory = currentMemory;
					}
				}
    		}
		}
    	int x = 0;
    	for (String head: header) {
    		Cell headcell = sheethead.createCell(x++);
    		headcell.setCellStyle(textStyle);
    		headcell.setCellValue(head);
		}
    	
    	// 最高值统计
		HSSFRow row1 = highSheet.createRow(0);
		row1.createCell(0).setCellValue("最高CPU占用率");
		row1.createCell(1).setCellValue(cpuZYL + "%");
		HSSFRow row2 = highSheet.createRow(1);
		row2.createCell(0).setCellValue("最高CPU等待磁盘io");
		row2.createCell(1).setCellValue(cpuIO + "%");
		HSSFRow row3 = highSheet.createRow(2);
		row3.createCell(0).setCellValue("最高内存已使用占比");
		row3.createCell(1).setCellValue(memory + "%");

        //文档输出
        FileOutputStream out = new FileOutputStream(System.getProperty("user.dir") + java.io.File.separator + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()).toString() +".xls");
        workbook.write(out);
        out.close();
	}
	
}
