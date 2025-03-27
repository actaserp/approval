package mes.app.mobile_notice.Service;

import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class SingleFileDownloadHandler extends FileDownloadHandler{


    @Override
    protected Resource createResource(String filePath, Object requestData) throws IOException {

        String fileName = requestData.toString();
        Path fullPath = Paths.get(filePath, fileName);

        if(Files.exists(fullPath)){
            return new InputStreamResource(new FileInputStream(fullPath.toFile()));
        }
        return null;
    }
}
