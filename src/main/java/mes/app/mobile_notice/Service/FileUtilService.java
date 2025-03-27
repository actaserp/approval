package mes.app.mobile_notice.Service;

import mes.app.mobile_notice.Service.FileDownloadHandler;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class FileUtilService {

    public FileDownloadHandler getFileDownloadHandler(List<Map<String, Object>> files){
        if(files.size() == 1) return new SingleFileDownloadHandler();
        return new ZipFileDownloadHandler();

    }

    public String generateFileName(List<Map<String, Object>> result, String displayName) {
        if(result.size() == 1) return result.get(0).get("fileornm").toString();
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        return today + displayName;
    }

    public Object prepareRequestData(List<Map<String, Object>> files){
        if(files.size() == 1) return files.get(0).get("filesvnm").toString();
        return files.stream().map(file -> {
            Map<String, String> fileMap = new HashMap<>();
            fileMap.put("filesvnm", file.get("filesvnm").toString());
            fileMap.put("fileornm", file.get("fileornm").toString());
            return fileMap;
        }).collect(Collectors.toList());
    }
}
