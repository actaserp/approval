package mes.app.mobile_notice.Service;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ZipFileDownloadHandler extends FileDownloadHandler {
    @Override
    protected Resource createResource(String filePath, Object requestData) throws IOException {


        List<Map<String, String>> fileList = (List<Map<String, String>>) requestData;

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ZipOutputStream zipOutputStream = new ZipOutputStream(byteArrayOutputStream);

        boolean fileFount = false;

        for(Map<String, String> file : fileList){
            String filesvnm = file.get("filesvnm");
            String fileornm = file.get("fileornm");

            Path fullPath = Paths.get(filePath, filesvnm);
            if(Files.exists(fullPath)){
                fileFount = true;
                try(FileInputStream fileInputStream = new FileInputStream(fullPath.toFile())){
                    ZipEntry zipEntry = new ZipEntry(fileornm);
                    zipOutputStream.putNextEntry(zipEntry);

                    byte[] buffer = new byte[1024]; // 1KB
                    int len;
                    while((len = fileInputStream.read(buffer)) > 0){
                        zipOutputStream.write(buffer, 0, len);
                    }
                    zipOutputStream.closeEntry();
                }
            }
        }
        zipOutputStream.close();

        if(!fileFount){
            return null;
        }

        return new ByteArrayResource(byteArrayOutputStream.toByteArray());
    }
}
