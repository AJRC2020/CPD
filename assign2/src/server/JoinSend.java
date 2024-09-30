package server;

import java.io.*;
import java.net.*;
import java.util.*;
import java.nio.charset.StandardCharsets;


public class JoinSend implements Runnable{
    private String receiving_ip;
    private String sending_ip;
    private int port;
    private List<String> keys;
    private List<String> ips;
    private String key;
    private File[] files;
    private Thread thread;

    public JoinSend(String receiving_ip, String sending_ip, int port, String key) {
        this.receiving_ip = receiving_ip;
        this.sending_ip = sending_ip;
        this.port = port;
        this.key = key;
        this.ips = new ArrayList<>();
        this.keys = new ArrayList<>();
        File folder = new File(sending_ip + "/");
        files = folder.listFiles();
        File cluster = new File(sending_ip + "/cluster.txt");
        try {
            Scanner scan = new Scanner(cluster);
            while(scan.hasNextLine()) {
                String line = scan.nextLine();
                String[] split = line.split(" ");
                this.keys.add(split[0]);
                this.ips.add(split[1]);
            }
            scan.close();

        } catch(FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void run() {
        for (int i = 0; i < files.length; i++) {
            String filename = files[i].getName();
            String del = filename.substring(0, 7);
            if (!filename.equals("cluster.txt") && !filename.equals("log.txt") && !filename.equals("counter.txt") && !del.equals("deleted")) {
                String fileKey = filename.substring(0, filename.length() - 4);
                int[] sending = get3Best(fileKey);
                if (sending[2] == -1) sendPut(files[i], fileKey);
                if (ips.get(sending[2]).equals(sending_ip)) {
                    if(compareIps(keys.get(sending[2]), key, fileKey)) {
                        sendPut(files[i], fileKey);
                        files[i].delete();
                    }
                }
            }
        }
    }

    private int[] get3Best(String fileKey) {
        int[] res = {-1, -1, -1};
        long min1 = 0, min2 = 0, min3 = 0;
        int size = keys.get(0).getBytes(StandardCharsets.UTF_8).length;


        for (int i = 0; i < keys.size(); i++) {
            long diff = byteToLong(keys.get(i).getBytes(StandardCharsets.UTF_8)) - byteToLong(fileKey.getBytes(StandardCharsets.UTF_8));
            if (diff < 0)
                diff += Math.pow(2, size-1);
            if (i == 0) {
                min1 = diff;
                res[0] = 0;
            }
            else if (i == 1) {
                if (min1 > diff) {
                    min2 = min1;
                    min1 = diff;
                    res[1] = res[0];
                    res[0] = 1;
                }
                else {
                    min2 = diff;
                    res[1] = 1;
                }
            }
            else {
                if (min1 > diff) {
                    min3 = min2;
                    min2 = min1;
                    min1 = diff;
                    res[2] = res[1];
                    res[1] = res[0];
                    res[0] = i;
                }
                else if (min2 > diff) {
                    min3 = min2;
                    min2 = diff;
                    res[2] = res[1];
                    res[1] = i;
                }
                else if (i == 2) {
                    min3 = diff;
                    res[2] = 2;
                }
                else if(min3 > diff) {
                    min3 = diff;
                    res[2] = i;
                }
            }
        }

        return res;
    }

    private long byteToLong (byte[] bytes) {
    long result = 0;
    for (int i = 0; i < Long.BYTES; i++) {
        result <<= Byte.SIZE;
        result |= (bytes[i] & 0xFF);
    }
    return result;
    }

    private boolean compareIps(String key_old, String key_new, String key_file) {
        int size = key_file.getBytes(StandardCharsets.UTF_8).length;

        long diff_old = byteToLong(key_old.getBytes(StandardCharsets.UTF_8)) - byteToLong(key_file.getBytes(StandardCharsets.UTF_8));
        if (diff_old < 0)
            diff_old += Math.pow(2, size-1);

        long diff_new = byteToLong(key_new.getBytes(StandardCharsets.UTF_8)) - byteToLong(key_file.getBytes(StandardCharsets.UTF_8));
        if (diff_new < 0)
            diff_new += Math.pow(2, size-1);
        
        return diff_old > diff_new;
    }

    public void sendPut(File file, String fileKey) {
        String value = "";

        try {
            Scanner scanner = new Scanner(file);
            while(scanner.hasNextLine()) {
                value += scanner.nextLine();
                value += "\n";
            }
            scanner.close();
        } catch(FileNotFoundException e) {
            e.printStackTrace();
            return;
        }

        String message = "s " + fileKey + " " + value;

        try (Socket socket = new Socket(receiving_ip, port)) {

            OutputStream output = socket.getOutputStream();
            PrintWriter writer = new PrintWriter(output, true);
            writer.println(message);

            InputStream input = socket.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(input));

            String time = reader.readLine();

            System.out.println(time);

        } catch (UnknownHostException ex) {

            System.out.println("Server not found: " + ex.getMessage());

        } catch (IOException ex) {

            System.out.println("I/O error: " + ex.getMessage());
        }
    }

    public void start(){
        if(thread == null){
            thread = new Thread(this);
            thread.start();
        }
    }
}
