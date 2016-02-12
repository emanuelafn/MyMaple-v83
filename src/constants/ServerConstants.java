package constants;

import java.text.SimpleDateFormat;
import java.util.Date;

public class ServerConstants {
    
    // -----------------------Rate Configuration--------------------------
    public static int EXP_RATE = 500;
    public static int MESO_RATE = 250;
    public static final byte DROP_RATE = 3;
    public static final byte BOSS_DROP_RATE = 3;
    public static int RESPAWN_RATE = 1;
    // -----------------------Login Configuration-------------------------
    public static final byte NUM_WORLDS = 1;
    public static final byte FLAG = 3;
    public static final int CHANNEL_LOAD = 20;
    public static final boolean ENABLE_PIC = false;
    public static final String EVENT_MESSAGE = time() + ": Welcome to MyMaple!";
    public static final long RANKING_INTERVAL = 3600000;
    public static final boolean IS_TEST = false;
    public static final boolean enableCooldowns = false;
    // -----------------------Channel Configuration-----------------------
    public static String SERVER_MESSAGE = "Welcome to MyMaple! ~.  Rates: 500x/250x/1x || Please vote for us on the website! http://MyMaple.tk/";
    public static final String EVENTS = "automsg";
    public static final boolean ENABLE_QUESTS = false;
    // -----------------------IP Configuration----------------------------
    //public static String HOST = "eafn.noip.me";
    public static String HOST = "127.0.0.1";
    // -----------------------Debug Configuration-------------------------
    public static final boolean DEBUG = false;
    public static boolean isViewable = false;
    // -----------------------Database Configuration //db.properties------
    //public static String url = "jdbc:mysql://localhost:3306/ep?autoReconnect=true";
    //public static String user = "root";
    //public static String password = "";
    
    public static String time(){
    Date date = new Date();
    SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yy");
    String time = sdf.format(date);
    return time;
    }
    
    //public String revision = "5/26/2015 - 5/26/2015 0.1";
}