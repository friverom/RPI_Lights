/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package rpi_lights;

import common.PulseOutput;
import common.SunsetCalculator;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.logging.Level;
import java.util.logging.Logger;
import rpio_client.Net_RPI_IO;
import util.ReadTextFile;
import util.WriteTextFile;

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
    
    public Light_Task(String address) throws IOException{
        this.address=address;
        this.rpio = new Net_RPI_IO(this.address,30000);
        readSettings();
    }
    
    public Light_Task() throws IOException{
        this.address="localhost";
        this.rpio = new Net_RPI_IO(this.address,30000);
        readSettings();
        
    }
    
    public String start(){
        
        Thread task = new Thread(new lightTask(),"Light Task");
        task.start();
        return "Light Task started";
    }
   
    public String set_longitude(double lon) throws IOException{
        this.longitud=lon;
        saveSettings();
        return "Longitude set";
    }
    
    public String get_longitude(){
        String longitude = String.format("%.6f",this.longitud);
        return longitude;
    }
    public String set_latitude(double lat) throws IOException{
        this.latitud=lat;
        saveSettings();
        return "Latitude set";
    }
    public String get_latitude(){
        String lat = String.format("%.6f", this.latitud);
        return lat;
    }
    
    public String set_light_timer(int timer) throws IOException{
        int_light_timer=timer;
        saveSettings();
        return "Timer set";
    }
    public String getTimer(){
        return ""+this.int_light_timer;
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
        
        calc.setLocation(latitud, longitud);
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
    
    public String killThread() throws IOException{
        runFlag = false;
        saveSettings();
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
            rpio.releaseLock(LIGHTSTASK, TASKLEVEL, OB_LIGHTS);
            rpio.releaseLock(LIGHTSTASK, TASKLEVEL, INT_LIGHTS);
            rpio.releaseLock(LIGHTSTASK, TASKLEVEL, EXT_LIGHTS);
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
    
     /**
     * Reads variable setting for Light task.
     * @throws FileNotFoundException
     * @throws IOException 
     */
    private void readSettings() throws FileNotFoundException, IOException{
        String path = "/home/pi/NetBeansProjects/RPI_Lights/vars.txt";
        File file = new File(path);
       
        if(!file.exists()){
            file.createNewFile();
            saveSettings();
        }
        
        ReadTextFile rf = new ReadTextFile(path);
        String[] lines=rf.openFile();
        
        String text = null;
        String[] parts = lines[0].split(";",3);
        
        if(parts.length==3){
                int_light_timer=Integer.parseInt(parts[0]);
                latitud=Double.parseDouble(parts[1]);
                longitud=Double.parseDouble(parts[2]);
            }
      
    }
    
     /**
     * Saves variable settings to file
     * @return
     * @throws IOException 
     */
    private void saveSettings() throws IOException{
        
        String path = "/home/pi/NetBeansProjects/RPI_Lights/vars.txt";
        
        WriteTextFile write = new WriteTextFile(path,false);        
        String data="";
        data=int_light_timer+";"+latitud+";"+longitud+"\n";
        write.writeToFile(data);
    }
}
