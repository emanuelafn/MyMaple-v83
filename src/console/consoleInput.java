package console;

import client.MapleCharacter;
import static client.MapleCharacter.getDefault;
import client.MapleClient;
import static client.command.AdminCommand.execute;
import java.util.Scanner;

/**
 * @author Emanuel
 */
public class consoleInput {
    
    MapleClient consoleClient = new MapleClient(); //dummy client
    MapleCharacter consUser = getDefault(consoleClient); //default player
    char heading = '!'; //char
   
    public static void console() {
        Scanner kb = new Scanner(System.in);
        System.out.print("> ");
        String command = kb.next();
        String[] commandA = {command};
           
        //execute(consoleClient, commandA, heading);
        
    }
    
    public static void main(String[] args) {
        while(true){
            console();
        }
    }   
}