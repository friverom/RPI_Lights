/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package rpi_lights;

import common.PulseOutput;
import common.SunsetCalculator;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.logging.Level;
import java.util.logging.Logger;
import rpio_client.Net_RPI_IO;

/**
 *
 * @author Federico
 */
public class Light_Task {
    
    private static final int INT_LIGHTS = 3; //Interior light relay
    private static final int EXT_LIGHTS = 4; //Platform lights relay
    private static final int OB_LIGHTS = 5; //Obstruction Lights relay
    private static final int EXT_BUTTON = 3; //PLatform light ON/OFF 
    private static final int RDR_DOOR = 1; //Door sensor input port
    private static final int LIGHTSTASK = 3; //Task number
    private static final int TASKLEVEL = 5; //Task level
    
    private String address = "localhost";
    private boolean runFlag = true;
    private boolean intFlag = false;
    private int int_light_timer = 1;
    private int pltfrm_state = 0;
    private int obst_state=0; //Obstruction light state
    private boolean sunrise_flag=false;
    private boolean sunset_flag=false;
    private double latitud=10.599594;
    private double longitud=-66.997908;
    
    Net_RPI_IO rpio = null;
    Thread setTimer = null;
    private SunsetCalculator calc=new SunsetCalculator();
    
    public Light_Task(String address){
        this.address=address;
        this.rpio = new Net_RPI_IO(this.address,30000);
        
    }
    
    public Light_Task(){
        this.address="localhost";
        this.rpio = new Net_RPI_IO(this.address,30000);
        
    }
    
    public String start(){
        
        Thread task = new Thread(new lightTask(),"Light Task");
        task.start();
        return "Light Task started";
    }
   
    public String set_longitude(double lon){
        this.longitud=lon;
        return "Longitude set";
    }
    public String set_latitude(double lat){
        this.latitud=lat;
        return "Latitude set";
    }
    public String set_light_timer(int timer){
        int_light_timer=timer;
        return "Timer set";
    }
    
    public String plt_lights_on(){
        rpio.setRly(LIGHTSTASK, TASKLEVEL, EXT_LIGHTS);
        pltfrm_state = 2;
        return "Platform lights ON";
    }
    
    public String plt_lights_off(){
        rpio.resetRly(LIGHTSTASK, TASKLEVEL, EXT_LIGHTS);
        pltfrm_state=0;
        return "Platform lights OFF";
    }
    
    public String obs_lights_on(){
        rpio.setRly(LIGHTSTASK, TASKLEVEL, OB_LIGHTS);
        obst_state = 1;
        return "Obstruction lights ON";
    }
    
    public String obs_lights_off(){
        rpio.resetRly(LIGHTSTASK, TASKLEVEL, OB_LIGHTS);
        obst_state=0;
        return "Obstruction lights OFF";
    }
    
    public String getStatus(){
        return getReport();
    }
    public String getSunData() {
        SimpleDateFormat format1 = new SimpleDateFormat("HH:mm zzz");
        
        Calendar date = Calendar.getInstance();
        Calendar set = calc.getSunset(date);
        
        date = Calendar.getInstance();
        Calendar rise = calc.getSunrise(date);
        String sunset = format1.format(set.getTime());
        String sunrise = format1.format(rise.getTime());
        
        String data="Sunrise: "+sunrise;
        data=data+"\nSunset: "+sunset+"\n";
        return data;
    }
    
    public String getReport(){
        String report;
       
        report=getSunData();
        
        if(pltfrm_state==0){
            report=report+"Platform Lights: OFF\n";
        }
        else{
            report=report+"Platform Lights: ON\n";
        }
            
        if(obst_state==0){
            report=report+"Obstruction Lights: OFF\n";
        } 
        else{
            report=report+"Obstruction Lights: ON\n";
        }
        
        return report;
    }
    
    public String killThread(){
        runFlag = false;
        return "killed";
    }
    
    public class lightTask implements Runnable{

        @Override
        public void run() {
            //Lock output relays for light task
            rpio.setLock(LIGHTSTASK, TASKLEVEL, INT_LIGHTS);
            rpio.setLock(LIGHTSTASK, TASKLEVEL, EXT_LIGHTS);
            rpio.setLock(LIGHTSTASK, TASKLEVEL, OB_LIGHTS);
            
            calc.setLocation(latitud, longitud);
            
            while (runFlag) {
                
                interior_lights();
                platform_lights();
                obstruction_lights();

                try {
                    Thread.sleep(100);
                } catch (InterruptedException ex) {
                    Logger.getLogger(Light_Task.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    
    }
    
    private void interior_lights(){
        //Check if door is open
        if (!get_input(RDR_DOOR) && !intFlag) {
            setTimer = new Thread(new PulseOutput(LIGHTSTASK, TASKLEVEL, INT_LIGHTS, int_light_timer), "Light Timer");
            setTimer.start();
            intFlag = true;
        } else if (intFlag) {
            if (setTimer.getState() == Thread.State.TERMINATED) {
                intFlag = false;
            }
        }
    }
    
    private void platform_lights(){
        
        switch(pltfrm_state){
            case 0:
                if(get_input(EXT_BUTTON)){
                    pltfrm_state = 1;
                    rpio.setRly(LIGHTSTASK, TASKLEVEL, EXT_LIGHTS);
                }
                break;
            case 1:
                if(!get_input(EXT_BUTTON)){
                    pltfrm_state = 2;
                }
                break;
            case 2:
                if(get_input(EXT_BUTTON)){
                    pltfrm_state = 3;
                    rpio.resetRly(LIGHTSTASK, TASKLEVEL, EXT_LIGHTS);
                }
                break;
            case 3:
                if(!get_input(EXT_BUTTON)){
                    pltfrm_state=0;
                }
                break;
                
        }
    }
    
    private void obstruction_lights(){
    //  TimeZone tz1 = TimeZone.getTimeZone("GMT-4");
        Calendar date=Calendar.getInstance();
        Calendar sunrise=calc.getSunrise(date);
        
        date=Calendar.getInstance();
        Calendar sunset=calc.getSunset(date);
               
        date=Calendar.getInstance();
       /*
        System.out.println("Actual Time: "+date.getTime()+" "+date.getTimeInMillis());
        System.out.println("Sunset "+sunset.getTime()+" "+sunset.getTimeInMillis());
        System.out.println("Sunrise "+sunrise.getTime()+" "+sunrise.getTimeInMillis());*/
        
       sunrise_flag=date.getTimeInMillis()<sunrise.getTimeInMillis();
       sunset_flag=date.getTimeInMillis()>sunset.getTimeInMillis();
       
       //state machine to handle obstruction light.
        switch (obst_state) {

            //Initial state. Obstruction light OFF. Change state to 1 if actual time
            //is greater than today's sunset.
            case 0:
                if (sunset_flag) {
                    rpio.setRly(LIGHTSTASK,TASKLEVEL,OB_LIGHTS);
                    obst_state = 1;
                    System.out.println("Obstruction lights ON at "+date.getTime());
                }
                break;
            // Obstruction light ON and wait for midnight to change to state2
            case 1:
                if (sunrise_flag) {
                    obst_state = 2;
                }
                break;
            //Obstruction light ON and wait's for sunrise to turn OFF obstruction
            //light and Platform lights.
            case 2:
                if (!sunrise_flag) {
                    rpio.resetRly(LIGHTSTASK,TASKLEVEL,OB_LIGHTS);
                    rpio.resetRly(LIGHTSTASK,TASKLEVEL,EXT_LIGHTS);
                    obst_state = 0;
                    pltfrm_state=0;
                    System.out.println("Obstruction lights OFF at "+date.getTime());
                    System.out.println("Platform lights OFF at "+date.getTime());
                }
            default:
        }
    }
    
    private boolean get_input(int port){
        String resp = rpio.getInput(LIGHTSTASK, TASKLEVEL, port);
        String parts[] = resp.split(",");
        boolean status;
        if(parts.length==3){
            status=Boolean.parseBoolean(parts[2]);
        } else {
            status = true;
        }
        return status;
    }
}
