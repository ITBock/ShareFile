package WorkCode.ShareFile.domain;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;
import java.util.List;

public class ServerThread implements Runnable {
    static HashSet<String> commands = new HashSet<>();

    static {
        commands.add("ls");
        commands.add("touch");
        commands.add("mkdir");
        commands.add("rm");
        commands.add("cd");
        commands.add("cat");
        commands.add("vi");
        commands.add("exit");
        commands.add("help");
    }

    private File directory = new File("D:/vscode/Java_Code/src/WorkCode/ShareFile/resource");

    private ServerSocket serverSocket;

    ServerThread(ServerSocket serverSocket) {
        this.serverSocket = serverSocket;
    }

    @Override
    public void run() {
        if(!directory.isDirectory()) {
            System.out.println("文件夹路径不合法");
            return;
        }

        Socket socket = null;
        DataInputStream is = null;
        DataOutputStream os = null;
        try {
//            准备转接
            socket = serverSocket.accept();
//            转接成功
            is = new DataInputStream(socket.getInputStream());
            os = new DataOutputStream(socket.getOutputStream());
            System.out.println(socket.getRemoteSocketAddress() + "成功转接至" + socket.getLocalPort());

        } catch (IOException e) {
            System.out.println("端口" + serverSocket.getLocalPort() + "转接失败");
            return;
        }

        try {
            while(true) {
                String line = Connect.receive(is);

                String []pieces = line.split(" ", 2);
                if(!commands.contains(pieces[0]))
                    Connect.send(os, pieces[0] + "指令不存在");

                else if(pieces.length == 1 && commands.contains(pieces[0])) {
                    switch (pieces[0]) {
                        case "help" -> {
                            String msg = "ls\t---列出所有文件\n" +
                                    "touch 文件名\t---创建新文件\n" +
                                    "mkdir 目录名\t---创建新目录\n" +
                                    "rm 文件名\t---删除指定文件\n" +
                                    "cd 目录名\t---进入指定目录\n" +
                                    "cat 文件名\t---查看文件内容\n" +
                                    "vi 文件名\t---修改指定文件\n" +
                                    "exit\t---断开连接\n" +
                                    "help\t---显示当前内容";
                            Connect.send(os, msg);
                        }
                        case "ls" -> {
                            String[] files = directory.list();
                            int lf = files.length;
                            if (lf != 0) {
                                StringBuilder sb = new StringBuilder();
                                for (int i = 0; i < lf - 1; i++)
                                    sb.append(files[i]).append(" ");
                                sb.append(files[lf - 1]);
                                Connect.send(os, sb.toString());
                            } else Connect.send(os, "文件夹为空");
                        }
                        case "exit" -> {
                            Connect.send(os, "bye");
                            throw new Exception();
                        }
                        default -> {
                            Connect.send(os, pieces[0] + "缺少参数");
                        }
                    }
                }

                else if(pieces.length == 2 && commands.contains(pieces[0])) {
//                        文件存放目录
                    String []files = directory.list();
                    HashSet<String> fileSet;
                    if(files == null)
                        fileSet = new HashSet<>();
                    else fileSet = new HashSet<>(List.of(files));

                    switch (pieces[0]) {
                        case "ls" -> {
                            Connect.send(os, pieces[0] + "无需参数");
                        }
                        case "touch" -> {
                            if (new File(directory, pieces[1]).createNewFile())
                                Connect.send(os, "文件" + pieces[1] + "创建成功");
                            else Connect.send(os, "文件" + pieces[1] + "已存在");
                        }
                        case "mkdir" -> {
                            if (new File(directory, pieces[1]).mkdir())
                                Connect.send(os, "目录" + pieces[1] + "创建成功");
                            else Connect.send(os, "目录" + pieces[1] + "已存在");
                        }
                        case "rm" -> {
                            if (fileSet.contains(pieces[1])) {
                                if (new File(directory, pieces[1]).delete())
                                    Connect.send(os, "文件" + pieces[1] + "删除成功");
                                else Connect.send(os, "文件" + pieces[1] + "删除失败");
                            } else Connect.send(os, "文件" + pieces[1] + "不存在");
                        }
                        case "cd" -> {
                            if("..".equals(pieces[1]) || "../".equals(pieces[1])) {
                                File d = directory.getParentFile();
                                if(d != null) {
                                    directory = d;
                                    Connect.send(os, "返回" + directory.getName() + "目录");
                                }
                                else Connect.send(os, "当前已是根目录");
                            }
                            else if (fileSet.contains(pieces[1])) {
                                File d = new File(directory, pieces[1]);
                                if(d.isDirectory()) {
                                    directory = d;
                                    Connect.send(os, "进入" + pieces[1] + "目录");
                                }
                                else Connect.send(os, pieces[1] + "不是一个目录");
                            } else Connect.send(os, "目录" + pieces[1] + "不存在");
                        }
                        case "cat" -> {
                            if (fileSet.contains(pieces[1])) {
                                FileInputStream fis = new FileInputStream(new File(directory, pieces[1]));
                                byte []bf = new byte[1024];
                                int lf;
                                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                                while ((lf = fis.read(bf)) != -1) {
                                    bos.write(bf, 0, lf);
                                }
                                if(bos.size() > 0)
                                    Connect.send(os, bos.toString());
                                else Connect.send(os, "文件" + pieces[1] + "为空");
                                fis.close();
                                bos.close();
                            } else Connect.send(os, "文件" + pieces[1] + "不存在");
                        }
                        case "vi" -> {
                            if(fileSet.contains(pieces[1])) {
                                Connect.send(os, "multiInput");

                                String msg = Connect.receive(is);
                                if("wq".equals(msg)) {
                                    msg = Connect.receive(is);

                                    OutputStreamWriter bw = new OutputStreamWriter(new FileOutputStream(new File(directory, pieces[1])));
                                    bw.write(msg);
                                    bw.close();
                                    Connect.send(os, "文件" + pieces[1] + "修改成功");
                                }
                                else Connect.send(os, "文件" + pieces[1] + "未发生变动");
                            }
                            else Connect.send(os, "文件" + pieces[1] + "不存在");
                        }
                    }
                }

                else Connect.send(os, "指令使用错误");
            }
        } catch (Exception e) {
            try {
                is.close();
                os.close();
                socket.close();
                System.out.println(socket.getRemoteSocketAddress() + "已断开连接");
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }
}