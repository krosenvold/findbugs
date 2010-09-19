import java.io.FileOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

class BadZipFileCreator {

    public static void main(String args[]) throws Exception {
        ZipOutputStream zipfile = new ZipOutputStream(new FileOutputStream("foo.zip"));
        for (int i = 0; i < args.length; i++) {
            ZipEntry e = new ZipEntry(args[i]);
            zipfile.putNextEntry(e);
            zipfile.closeEntry();
        }
        zipfile.close();
    }
}
