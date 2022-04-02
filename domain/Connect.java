package WorkCode.ShareFile.domain;

import javax.xml.crypto.Data;
import java.io.*;
import java.nio.charset.StandardCharsets;

public class Connect {
    static byte []b = new byte[1024];
    static int len = 0;

    static void send(DataOutputStream dos, String msg) throws IOException {
//      传大小
        byte []bt = msg.getBytes(StandardCharsets.UTF_8);
        send(dos, bt);
    }
    static void send(DataOutputStream dos, byte[] bt) throws IOException {
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
        bos.close();
        return bos.toString(StandardCharsets.UTF_8);
    }
    static void receive(DataInputStream dis, FileOutputStream fos) throws IOException{
//      读大小
        int size = dis.readInt();

//      读数据
        while((len = dis.read(b, 0, Math.min(size, 1024))) > 0) {
            fos.write(b, 0, len);
            size -= len;
        }
    }
}
