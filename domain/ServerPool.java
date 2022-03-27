package WorkCode.ShareFile.domain;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServerPool {
    static int openPort = 8888;

    public static void main(String[] args) {
        ServerSocket ss = null;
        ExecutorService ex = Executors.newCachedThreadPool();
        try {
//        用于与多个客户端建立连接
            int closePort = openPort;
            ss = new ServerSocket(openPort);

            while (true) {
                Socket socket = ss.accept();
//            连接到客户端
                DataOutputStream os = new DataOutputStream(socket.getOutputStream());
                System.out.println("成功连接至客户端" + socket.getRemoteSocketAddress());

//            转接到新端口
                while (true) {
//                尝试开新端口
                    try {
                        closePort = (++closePort) % 65536;
                        if(closePort == 0)
                            closePort += 1024;
                        ServerSocket serverSocket = new ServerSocket(closePort);
//                    创建新线程与客户端连接
                        System.out.println("正在转接至端口" + closePort + "...");

                        ex.execute(new ServerThread(serverSocket));
//                    告诉客户端转接端口
                        os.writeInt(closePort);

                        os.close();
                        socket.close();
                        break;
                    } catch (IOException e) {
                        System.out.println(closePort + "端口不可用");
                    }
                }
            }
        } catch (IOException e) {
            try {
                ex.shutdown();
                if(ss != null)
                    ss.close();
                System.out.println("服务器异常");
            } catch (IOException exc) {
                exc.printStackTrace();
            }
        }

    }
}
