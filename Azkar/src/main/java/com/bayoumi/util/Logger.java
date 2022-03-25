package com.bayoumi.util;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Used to log Exceptions and Information
 *
 * @author Abdelrahman Bayoumi
 */
public class Logger {

    //========= Helper Objects =========
    private static final SimpleDateFormat DATE_TIME_FORMAT = new SimpleDateFormat("[dd-MM-yyyy] [hh:mm:ss.SSS a]");
    private static final Object LOCK = new Object();
    private static PrintWriter PRINT_WRITER;

    /**
     * initialize printWriter object to log data in a file
     */
    private Logger() {
    }

    public static void init() {
        try {
            PRINT_WRITER = new PrintWriter(new FileWriter(Constants.assetsPath + "/logs/debug.txt", true));
        } catch (IOException ex) {
            System.err.println(ex.getLocalizedMessage());
        }
    }

    public static void info(String msg) {
        new Thread(() -> {
            synchronized (LOCK) {
                String DataAndTime = DATE_TIME_FORMAT.format(new Date());
                System.out.println(DataAndTime + " => " + msg);
                PRINT_WRITER.println(DataAndTime + " => " + msg);
                PRINT_WRITER.flush();
            }
        }).start();
    }


    public static void error(String msg, Throwable throwable, String CLASS_NAME) {
        new Thread(() -> {
            synchronized (LOCK) {
                if (Constants.RUNNING_MODE.equals(Constants.Mode.DEVELOPMENT)) {
                    throwable.printStackTrace();
                }
                String DataAndTime = DATE_TIME_FORMAT.format(new Date());
                String m = DataAndTime + " => "
                        + "Exception[ " + throwable.getLocalizedMessage() + " ] in => "
                        + CLASS_NAME;
                m += (msg != null) ? (" => " + msg) : "";
                System.err.println(m);
                PRINT_WRITER.println(m);
                PRINT_WRITER.flush();
            }
        }).start();
    }
}
