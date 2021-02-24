package com.ggq.monitortoexl;

import com.ggq.monitortoexl.service.MonitorService;

/**
 * Hello world!
 *
 */
public class App 
{
    public static void main( String[] args ) throws Exception
    {
    	new MonitorService().getServerInfoToExl();
    }
}
