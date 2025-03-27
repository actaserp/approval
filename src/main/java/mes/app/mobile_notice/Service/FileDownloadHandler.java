package mes.app.mobile_notice.Service;


import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public abstract class FileDownloadHandler {

    public ResponseEntity<Resource> download(String filepath, Object requestData, String displayFileName){

        try{
            Resource resource = createResource(filepath, requestData);

            if(resource == null){
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
            }

            String encodedFileName = URLEncoder.encode(displayFileName, StandardCharsets.UTF_8).replaceAll("\\+", "%20");

            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFileName);
            headers.add("Access-Control-Expose-Headers", "Content-Disposition");
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(resource);

        }catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    protected abstract Resource createResource(String filePath, Object requestData)throws IOException;
}
