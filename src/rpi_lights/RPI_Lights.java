/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package rpi_lights;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Federico
 */
public class RPI_Lights {

    static Light_Task lights = null;
    static ServerSocket serversocket = null;
    static Socket socket = null;
    static InputStream in = null;
    static BufferedReader input = null;
    static PrintWriter output = null;
    static boolean runFlag = true;
    
    public static void main(String[] args) throws IOException {
    
        if (args.length==0){
            lights = new Light_Task();
        } else {
            lights = new Light_Task(args[0]);
        }
        lights.start();    
        
         //Start to listen on port 30003 for commands
        try {
            serversocket = new ServerSocket(30003);
        } catch (IOException ex) {
            Logger.getLogger(RPI_Lights.class.getName()).log(Level.SEVERE, null, ex);
        }
        // Loop until Kill Thread command received
        while(runFlag){
            try {
                waitRequest(); //Wait for command and process request.
            } catch (IOException ex) {
                Logger.getLogger(RPI_Lights.class.getName()).log(Level.SEVERE, null, ex);
            }
            
        }
        serversocket.close();
    }
    
    /**
     * This method wait for a connection, get the command.
     * @throws IOException 
     */
    private static void waitRequest() throws IOException {
       
        String request = "";
        String reply = "";
        socket = serversocket.accept();
        in = socket.getInputStream();
        input = new BufferedReader(new InputStreamReader(in));
        output = new PrintWriter(socket.getOutputStream(), true);
        request = input.readLine(); //Get Command
        reply = processRequest(request); //Process command
        output.println(reply);
        input.close();
        output.close();
    }
    
     /**
     * This method Process the command
     * @param request
     * @return 
     */
    private static String processRequest(String request){
        String reply="";
        String command="";
        double data=0;
        
        String parts[]=request.split(",");
        
        if(parts.length==1){
            command=request;
        }else{
            command=parts[0];
            data=Double.parseDouble(parts[1]);
        }
        
        switch(command){
            case "get status":
                reply=getStatus();
                break;
            case "kill thread":
                runFlag = false;
                reply=lights.killThread();
                break;
            case "set timer":
                reply=lights.set_light_timer((int) data);
                break;
            case "platform on":
                reply=lights.plt_lights_on();
                break;
            case "platform off":
                reply=lights.plt_lights_off();
                break;
            case "obstruction on":
                reply=lights.obs_lights_on();
                break;
            case "obstruction off":
                reply=lights.obs_lights_off();
                break;
            case "set latitude":
                reply=lights.set_latitude(data);
                break;
            case "set longitude":
                reply=lights.set_longitude(data);
                break;
                
            default:
                reply="invalid command";
        }
        return reply;
    }
    
    public static String getStatus(){
        return lights.getStatus();
    }
    
}
