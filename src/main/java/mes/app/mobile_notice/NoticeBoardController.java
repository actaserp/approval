package mes.app.mobile_notice;


import mes.app.MobileUsr.AppInfo.PagingService;
import mes.app.mobile_notice.Service.FileDownloadHandler;
import mes.app.mobile_notice.Service.FileUtilService;
import mes.app.mobile_notice.Service.NoticeBoardService;
import mes.config.Settings;
import mes.domain.DTO.FileResponseDto;
import mes.domain.DTO.NoticeResponseDto;
import mes.domain.repository.sportsRepository.FILEINFORepository;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import javax.persistence.EntityNotFoundException;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/notice")
public class NoticeBoardController {

    private final NoticeBoardService noticeBoardService;
    private final PagingService pagingService;
    private final FILEINFORepository fileRepository;
    private final Settings settings;
    private final String filepath;
    private final FileUtilService fileUtilService;


    public NoticeBoardController(NoticeBoardService noticeBoardService, PagingService pagingService, FILEINFORepository fileRepository, Settings settings, FileUtilService fileUtilService) {
        this.noticeBoardService = noticeBoardService;

        this.pagingService = pagingService;
        this.fileRepository = fileRepository;
        this.settings = settings;
        filepath = settings.getProperty("file_upload_path");
        this.fileUtilService = fileUtilService;
    }

    @GetMapping("/list")
    public Map<String, Object> NoticeList(Pageable pageable,
                                          @RequestParam(required = false) String searchKeyword){

        Map<String, Object> items = new HashMap<>();

        Page<NoticeResponseDto> noticeResponseDtos = noticeBoardService.NoticeList(pageable, searchKeyword);
        List<Integer> pageList = pagingService.pagingList(noticeResponseDtos.getNumber(), noticeResponseDtos.getTotalPages());

        items.put("notice", noticeResponseDtos);
        items.put("page", pageList);

        return items;
    }

    @PostMapping("/download")
    public ResponseEntity<Resource> downloadFile(@RequestBody Map<String, Integer> requestData){

        Integer id = requestData.get("id");

        List<FileResponseDto> fileList = noticeBoardService.NoticeFile(id);

        if (fileList.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }

        String path = filepath + "공지사항/";

        List<Map<String, Object>> result = fileList.stream().map(file -> {
            Map<String, Object> map = new HashMap<>();

            map.put("filesvnm", file.saveNm());
            map.put("fileornm", file.originalName());
            return map;
        }).toList();


        FileDownloadHandler handler = fileUtilService.getFileDownloadHandler(result);

        Object request = fileUtilService.prepareRequestData(result);

        String fileName = fileUtilService.generateFileName(result, "_공지사항첨부파일.zip");

        return handler.download(path, request, fileName);
    }


}
