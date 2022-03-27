package WorkCode.ShareFile.domain;

import java.io.*;

public class Connect {
    static byte []b = new byte[1024];
    static int len = 0;

    static void send(DataOutputStream dos, String msg) throws IOException {
//      传大小
        byte []bt = msg.getBytes();
        int size = bt.length;
        dos.writeInt(size);
        dos.flush();

//      传数据
        ByteArrayInputStream bis = new ByteArrayInputStream(bt);
        while((len = bis.read(b)) != -1)
            dos.write(b, 0, len);
        dos.flush();
    }

    static String receive(DataInputStream dis) throws IOException {
//      读大小
        int size = dis.readInt();

//      读数据
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        while((len = dis.read(b, 0, Math.min(size, 1024))) > 0) {
            bos.write(b, 0, len);
            size -= len;
        }
        return bos.toString();
    }
}
