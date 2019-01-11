/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package common;

import java.util.logging.Level;
import java.util.logging.Logger;
import rpio_client.Net_RPI_IO;

/**
 *
 * @author Federico
 */
public class PulseOutput implements Runnable {
    
    private Net_RPI_IO rpio=new Net_RPI_IO("localhost",30000);
    private int relay=0;
    private int timer=0;
    private int task;
    private int level;
    
    public PulseOutput(int task, int level, int relay, int timer){
        
        this.relay=relay;
        this.timer=timer;
        this.task=task;
        this.level=level;
    }

    @Override
    public void run() {
        
        rpio.setRly(task,level,relay);
        try {
            Thread.sleep(timer*60*1000);
        } catch (InterruptedException ex) {
            Logger.getLogger(PulseOutput.class.getName()).log(Level.SEVERE, null, ex);
        }
        rpio.resetRly(task,level,relay);
    }
    
}
