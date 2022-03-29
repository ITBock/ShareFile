package WorkCode.ShareFile.domain;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;
import java.util.List;

public class ServerThread implements Runnable {
    private static HashSet<String> commands = new HashSet<>();

    private static final String rootPath = "D:\\vscode\\Java_Code\\src\\WorkCode\\ShareFile\\resource";

    static {
        commands.add("ls");
        commands.add("touch");
        commands.add("mkdir");
        commands.add("rm");
        commands.add("cd");
        commands.add("cat");
        commands.add("cp");
        commands.add("mv");
        commands.add("scp");
        commands.add("vi");
        commands.add("exit");
        commands.add("help");
    }

    private File directory = new File(rootPath);
    private int depth = 0;

    private ServerSocket serverSocket;

    public ServerThread(ServerSocket serverSocket) {
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

                String []pieces = line.split(" +", 3);
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
                                    "cp 源文件路径 目标目录路径\t---拷贝文件\n" +
                                    "mv 源文件路径 目标目录路径\t---移动文件\n" +
                                    "scp 本地文件路径 远程目录路径\t---从本地拷贝到远程\n" +
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

                    switch (pieces[0]) {
                        case "touch" -> {
                            if(pieces[1].contains("/"))
                                Connect.send(os, "文件名不可带有\"/\"");
                            else if (new File(directory, pieces[1]).createNewFile())
                                Connect.send(os, "文件" + pieces[1] + "创建成功");
                            else Connect.send(os, "文件" + pieces[1] + "已存在");
                        }
                        case "mkdir" -> {
                            if(pieces[1].contains("/"))
                                Connect.send(os, "目录名不可带有\"/\"");
                            else if (new File(directory, pieces[1]).mkdir())
                                Connect.send(os, "目录" + pieces[1] + "创建成功");
                            else Connect.send(os, "目录" + pieces[1] + "已存在");
                        }
                        case "rm" -> {
                            File file = new File(directory, pieces[1]);

                            if (file.exists()) {
                                if (new File(directory, pieces[1]).delete())
                                    Connect.send(os, "文件" + pieces[1] + "删除成功");
                                else Connect.send(os, "文件" + pieces[1] + "删除失败");
                            } else Connect.send(os, "文件" + pieces[1] + "不存在");
                        }
                        case "cd" -> {
                            String msg = null;
                            File tDirectory = new File(msg = locateDirectory(pieces[1]));

                            if(tDirectory.isDirectory()) {
                                directory = tDirectory;
                                depth = 0;
                                while(!rootPath.equals(tDirectory.getAbsolutePath())) {
                                    tDirectory = tDirectory.getParentFile();
                                    depth++;
                                }
                                if(depth == 0) {
                                    Connect.send(os, "当前位于根目录下");
                                }
                                else Connect.send(os, "当前位于" + directory.getName() + "目录下");
                            }
                            else Connect.send(os, msg);
                        }
                        case "cat" -> {
                            File file = new File(directory, pieces[1]);

                            if (file.exists()) {
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
                            File file = new File(directory, pieces[1]);

                            if(file.exists()) {
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
                        default -> {
                            Connect.send(os, pieces[0] + "无需参数");
                        }
                    }
                }
                else if(pieces.length == 3 && commands.contains(pieces[0])) {
                    switch (pieces[0]) {
                        case "cp" -> {
                            String msg = null;
                            File source = new File(msg = locateFile(pieces[1]));
                            if(source.isFile()) {
                                File target = new File(msg = locateDirectory(pieces[2]));
                                if(target.isDirectory()) {
                                    FileInputStream bis = new FileInputStream(source);
                                    FileOutputStream bos = new FileOutputStream(new File(target, source.getName()));
                                    bos.write(bis.readAllBytes());
                                    bis.close();
                                    bos.close();
                                    Connect.send(os, "文件" + source.getName() + "拷贝成功");
                                }
                                else Connect.send(os, msg);
                            }
                            else Connect.send(os, msg);
                        }
                        case "mv" -> {
                            String msg = null;
                            File source = new File(msg = locateFile(pieces[1]));
                            if(source.isFile()) {
                                File target = new File(msg = locateDirectory(pieces[2]));
                                if(target.isDirectory()) {
                                    FileInputStream bis = new FileInputStream(source);
                                    FileOutputStream bos = new FileOutputStream(new File(target, source.getName()));
                                    bos.write(bis.readAllBytes());
                                    bis.close();
                                    bos.close();
                                    if(source.delete()) {
                                        Connect.send(os, "文件" + source.getName() + "移动成功");
                                    }else Connect.send(os, "文件" + source.getName() + "移动失败");
                                }
                                else Connect.send(os, msg);
                            }
                            else Connect.send(os, msg);
                        }
                        case "scp" -> {
                            String msg = null;
                            File target = new File(msg = locateDirectory(pieces[2]));
                            if(target.isDirectory()) {
                                Connect.send(os, "upload");
                                String name = Connect.receive(is);
                                if("/".equals(name)) {
                                    Connect.send(os, "本地文件路径不存在");
                                    break;
                                }

                                File file = new File(target, name);
                                FileOutputStream bos = new FileOutputStream(file);

                                msg = Connect.receive(is);
                                bos.write(msg.getBytes());

                                bos.close();
                                Connect.send(os, "文件" + name + "上传完成");
                            }
                            else Connect.send(os, msg);
                        }
                        default -> {
                            Connect.send(os, pieces[0] + "存在多余参数");
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

    String locateFile(String navigation) {
        File tDirectory = directory;
        int tDepth = depth;
        if(navigation.charAt(0) == '/') {
            tDirectory = new File(rootPath);
            tDepth = 0;
            navigation = navigation.substring(1);
        }
        String []ds = navigation.split("/+");
        int level = ds.length;
        for(int i = 0; i < level; i++) {
            String d = ds[i];
            if(".".equals(d) || "".equals(d)) {continue;}

            File file = new File(tDirectory, d);

            if("..".equals(d)) {
                if(tDepth > 0) {
                    tDirectory = tDirectory.getParentFile();
                    tDepth--;
                }
                else return "根目录不可返回上一层";
            }
            else if (file.exists()) {
                if(file.isDirectory() || i == level - 1) {
                    tDirectory = file;
                    tDepth++;
                }
                else return d + "不是一个目录";
            } else return "目录" + d + "不存在";
        }
        if(tDirectory.isDirectory())
            return tDirectory.getName() + "不是一个文件";
        else return tDirectory.getAbsolutePath();
    }

    String locateDirectory(String navigation) {
        File tDirectory = directory;
        int tDepth = depth;
        if(navigation.charAt(0) == '/') {
            tDirectory = new File(rootPath);
            tDepth = 0;
            navigation = navigation.substring(1);
        }
        String []ds = navigation.split("/+");
        int level = ds.length;
        for(int i = 0; i < level; i++) {
            String d = ds[i];
            if(".".equals(d) || "".equals(d)) {continue;}

            File file = new File(tDirectory, d);

            if("..".equals(d)) {
                if(tDepth > 0) {
                    tDirectory = tDirectory.getParentFile();
                    tDepth--;
                }
                else return "根目录不可返回上一层";
            }
            else if (file.exists()) {
                if(file.isDirectory()) {
                    tDirectory = file;
                    tDepth++;
                }
                else return d + "不是一个目录";
            } else return "目录" + d + "不存在";
        }
        return tDirectory.getAbsolutePath();
    }
}