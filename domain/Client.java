package WorkCode.ShareFile.domain;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Scanner;

public class Client {
    public static void main(String[] args) {
//        等待转接
        Socket socket = null;

        try {
//            socket = new Socket("112.74.37.10", 8888);
            socket = new Socket(InetAddress.getLocalHost(), 8888);
            System.out.println("成功连接至服务器");
            System.out.println("正在转接...");

        } catch (IOException e) {
            System.out.println("无法连接至服务器");
            return;
        }

        byte b[] = new byte[5];
        int len = 0;
        int realPort = 0;
        DataInputStream is = null;
        DataOutputStream os = null;

        try {
            is = new DataInputStream(socket.getInputStream());
            realPort = is.readInt();

//        转接端口
            is.close();
            socket.close();
            socket = new Socket(InetAddress.getLocalHost(), realPort);

            is = new DataInputStream(socket.getInputStream());
            os = new DataOutputStream(socket.getOutputStream());

            System.out.println("转接成功");

        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("转接失败");
            return;
        }

        try {
            Scanner scanner = new Scanner(System.in);
            String req = null;
            String msg = null;
            while (true) {
//              输出本机地址
                System.out.print("\033[93m" + socket.getLocalSocketAddress() + "$\033[m ");

//              向服务器发信息
                req = scanner.nextLine();
                Connect.send(os, req);

//              等待服务器返回
                msg = Connect.receive(is);
                if("multiInput".equals(msg)) {
//                  vi编辑模式
                    System.out.println("\033[93m多行编辑模式(末行输入\":wq\"保存退出,\":q\"不保存退出):\033[m");
                    StringBuilder sb = new StringBuilder();
                    while(true) {
                        req = scanner.nextLine();
                        if(":q".equals(req) || ":wq".equals(req))
                            break;
                        sb.append(req).append("\n");
                    }

//                  vi二次通信
                    String s = req.substring(1);
                    Connect.send(os, s);

                    if("wq".equals(s)) {
//                      vi三次通信
                        req = sb.substring(0, sb.length() - 1);
                        Connect.send(os, req);
                    }
                    msg = Connect.receive(is);
                }
                if("upload".equals(msg)) {
                    File file = new File(req.split(" +")[1]);
                    if(file.isFile()) {
                        Connect.send(os, file.getName());

                        FileInputStream fis = new FileInputStream(file);
                        Connect.send(os, new String(fis.readAllBytes()));
                    } else Connect.send(os, "/");

                    msg = Connect.receive(is);
                }

                System.out.println(msg);
                if("exit".equals(req) && "bye".equals(msg))
                    throw new Exception();
            }

        } catch (Exception e) {
            try {
                is.close();
                os.close();
                socket.close();
                System.out.println("\033[31m已断开连接");
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }
}
