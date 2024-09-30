package server;

import java.util.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;


public class LeaveSend implements Runnable {
    private List<String> ips;
    private List<String> keys;
    private File[] files;
    private String tcp_ip;
    private int tcp_port;
    private Thread thread;

    public LeaveSend(List<String> ips, List<String> keys , File[] files, int tcp_port, String tcp_ip) {
        this.ips = ips;
        this.keys = keys;
        this.files = files;
        this.tcp_port = tcp_port;
        this.tcp_ip = tcp_ip;
    }

    public void run() {
        for (int i = 0; i < files.length; i++) {
            String filename = files[i].getName();
            String del = filename.substring(0, 7);
            if (filename.equals("cluster.txt")) {
                File temp = new File("temp.txt");
                Scanner reader;
                try {
                    reader = new Scanner(files[i]);
                }catch(FileNotFoundException e) {
                    e.printStackTrace();
                    continue;
                }
                String line = reader.nextLine();
                try {
                    FileWriter writer = new FileWriter(temp, true);
                    writer.write(line);
                    writer.close();
                } catch(IOException e) {
                    e.printStackTrace();
                    continue;
                }
                reader.close();
                temp.renameTo(files[i]);
            }
            else if (filename.equals("counter.txt")) {
                try {
                    FileWriter writer = new FileWriter(files[i], false);
                    writer.write("1");
                    writer.close();
                } catch(IOException e) {
                    e.printStackTrace();
                    continue;
                }
            }
            else if (filename.equals("log.txt")) {
                if (files[i].delete()) {
                    try{
                        File log = new File(tcp_ip + "/log.txt");
                        FileWriter writer = new FileWriter(log);
                        writer.write("");
                        writer.close();
                    } catch(IOException e) {
                        e.printStackTrace();
                        continue;
                    }
                }   
                else continue;
            }
            else if (!del.equals("deleted")) {
                String content = "";
                Scanner reader;
                try {
                    reader = new Scanner(files[i]);
                }catch(FileNotFoundException e) {
                    e.printStackTrace();
                    continue;
                }
                while (reader.hasNextLine()) {
                    content += reader.nextLine();
                    content += "\n";
                }
                reader.close();

                String key = filename.substring(0, filename.length()-4);
                int[] best = get3Best(key);
                sendPut(key, content, ips.get(best[2]));
                files[i].delete();
            }
        }


    }

    private int[] get3Best(String key) {
        int[] res = {-1, -1, -1};
        long min1 = 0, min2 = 0, min3 = 0;
        int size = keys.get(0).getBytes(StandardCharsets.UTF_8).length;


        for (int i = 0; i < keys.size(); i++) {
            long diff = byteToLong(keys.get(i).getBytes(StandardCharsets.UTF_8)) - byteToLong(key.getBytes(StandardCharsets.UTF_8));
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

    private void sendPut(String key, String value, String ip) {
        String message = "s " + key + " " + value;

        try (Socket socket = new Socket(ip, tcp_port)) {

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
