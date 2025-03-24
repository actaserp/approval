package mes.app.PaymentList;

import lombok.extern.slf4j.Slf4j;
import mes.app.PaymentList.service.PaymentDetailService;
import mes.domain.entity.User;
import mes.domain.model.AjaxResult;
import mes.domain.repository.approval.TB_AA010ATCHRepository;
import mes.domain.repository.approval.tb_aa010Repository;
import org.aspectj.weaver.loadtime.Aj;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Slf4j
@RestController
@RequestMapping("/api/PaymentDetail")
public class PaymentDetailController {

  @Autowired
  PaymentDetailService paymentDetailService;

  @Autowired
  tb_aa010Repository tbAa010PdfRepository;

  @Autowired
  TB_AA010ATCHRepository tbAa010AtchRepository;

  @GetMapping("/read")
  public AjaxResult getPaymentList(@RequestParam(value = "startDate") String startDate,
                                   @RequestParam(value = "endDate") String endDate,
                                   @RequestParam(value = "search_spjangcd", required = false) String spjangcd,
                                   @RequestParam(value = "SearchPayment", required = false) String SearchPayment,
                                   @RequestParam(value = "searchUserNm", required = false) String searchUserNm,
                                   Authentication auth) {
    AjaxResult result = new AjaxResult();
    log.info("주문 확인 read 들어온 데이터:startDate{}, endDate{}, spjangcd {}, SearchPayment {} ,searchUserNm {} ", startDate, endDate, spjangcd, SearchPayment,searchUserNm);

    try {
      // 데이터 조회
      User user = (User) auth.getPrincipal();
      String agencycd = user.getAgencycd().replaceFirst("^p", "");
      List<Map<String, Object>> getPaymentList = paymentDetailService.getPaymentList(spjangcd, startDate, endDate, SearchPayment,searchUserNm,agencycd);

      for (Map<String, Object> item : getPaymentList) {
        //날짜 포맷 변환 (repodate)
        formatDateField(item, "repodate");
        //날짜 포맷 변환 (appdate)
        formatDateField(item, "appdate");

     /*   //papercd 값이 "101"이면 "전표결재(지출결의서)"
        if ("101".equals(item.get("papercd"))) {
          item.put("papercd", "전표결재(지출결의서)");
        }*/

        String appnum = (String) item.get("appnum");
        if (appnum != null) {
          List<Map<String, Object>> fileList = new ArrayList<>();

          if (appnum.startsWith("AS")) {
            if (fileExistsInAtchTable(appnum)) {
              fileList.add(createFileMapFromAtch(appnum, "첨부파일"));
            }
            if (fileExistsInPdfTable(appnum)) {
              fileList.add(createFileMapFromPdf(appnum, "지출결의서"));
            }
          } else if (appnum.startsWith("A")) {
            if (fileExistsInAtchTable(appnum)) {
              fileList.add(createFileMapFromAtch(appnum, "첨부파일"));
            }
          } else if (appnum.startsWith("S")) {
            if (fileExistsInPdfTable(appnum)) {
              fileList.add(createFileMapFromPdf(appnum, "지출결의서"));
            }
          } else {
            if (fileExistsInPdfTable(appnum)) {
              fileList.add(createFileMapFromPdf(appnum, "전표파일"));
            }
          }

          item.put("fileList", fileList);
          item.put("isdownload", !fileList.isEmpty());
        }
      }

      // 데이터가 있을 경우 성공 메시지
      result.success = true;
      result.message = "데이터 조회 성공";
      result.data = getPaymentList;

    } catch (Exception e) {
      // 예외 처리
      result.success = false;
      result.message = "데이터 조회 중 오류 발생: " + e.getMessage();
    }

    return result;
  }

  @GetMapping("/read1")
  public AjaxResult getPaymentList1(@RequestParam(value = "startDate") String startDate,
                                    @RequestParam(value = "endDate") String endDate,
                                    @RequestParam(value = "search_spjangcd", required = false) String spjangcd,
                                    Authentication auth) {
    AjaxResult result = new AjaxResult();
    log.info("결재목록_문서현황 read 들어온 데이터:startDate{}, endDate{}, spjangcd {} ", startDate, endDate, spjangcd);

    try {

      User user = (User) auth.getPrincipal();
      String agencycd = user.getAgencycd().replaceFirst("^p", "");
      String userName = user.getFirst_name();
      // 데이터 조회
      List<Map<String, Object>> getPaymentList = paymentDetailService.getPaymentList1(spjangcd, startDate, endDate,agencycd);


      // 데이터가 있을 경우 성공 메시지
      result.success = true;
      result.message = "데이터 조회 성공";
      result.data = Map.of(
          "userName", userName,  // 사용자 이름
          "paymentList", getPaymentList // 결재 목록 리스트
      );

    } catch (Exception e) {
      // 예외 처리
      result.success = false;
      result.message = "데이터 조회 중 오류 발생: " + e.getMessage();
    }

    return result;
  }

  // 날짜 포맷
  private void formatDateField(Map<String, Object> item, String fieldName) {
    Object dateValue = item.get(fieldName);
    if (dateValue instanceof String) {
      String dateStr = (String) dateValue;
      try {
        if (dateStr.length() == 8) { // "yyyyMMdd" 형식인지 확인
          String formattedDate = dateStr.substring(0, 4) + "-" + dateStr.substring(4, 6) + "-" + dateStr.substring(6, 8);
          item.put(fieldName, formattedDate);
        } else {
          item.put(fieldName, "잘못된 날짜 형식");
        }
      } catch (Exception ex) {
        log.error("{} 변환 중 오류 발생: {}", fieldName, ex.getMessage());
        item.put(fieldName, "잘못된 날짜 형식");
      }
    }
  }

  @RequestMapping(value = "/pdf", method = RequestMethod.GET)
  public ResponseEntity<Resource> getPdf(@RequestParam("appnum") String appnum) {
    try {
      log.info("PDF 조회 요청: appnum={}", appnum);

      // DB에서 PDF 파일명 조회
      Optional<String> optionalPdfFileName = paymentDetailService.findPdfFilenameByRealId(appnum);
      if (optionalPdfFileName.isEmpty()) {
        log.warn("PDF 파일명을 찾을 수 없음: appnum={}", appnum);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
      }

      // 파일명 그대로 사용
      String pdfFileName = optionalPdfFileName.get();
      log.info("사용 파일명: {}", pdfFileName);

      // 운영체제별 저장 경로 설정
      String osName = System.getProperty("os.name").toLowerCase();
      String uploadDir = osName.contains("win") ? "C:\\Temp\\APP\\S_KRU\\"
          : System.getProperty("user.home") + "/APP/S_KRU";

      // PDF 파일 경로 설정 및 존재 여부 확인
      Path pdfPath = Paths.get(uploadDir, pdfFileName);
      log.info("PDF 파일 경로: {}", pdfPath.toString());

      if (!Files.exists(pdfPath)) {
        log.warn("파일이 존재하지 않음: {}", pdfPath.toString());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
      }

      // 파일 정보 로깅
      File file = pdfPath.toFile();
      log.info("파일 존재 확인 완료 - 파일 크기: {} bytes", file.length());

      // PDF 파일을 Resource로 변환 후 응답
      Resource resource = new FileSystemResource(file);
      log.info("Resource 변환 완료, 파일 응답 준비 시작");

      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_PDF);
      headers.setContentDisposition(ContentDisposition.inline().filename(pdfFileName, StandardCharsets.UTF_8).build());

      // `X-Frame-Options` 제거 (필요한 경우 추가 가능)
      headers.add("X-Frame-Options", "ALLOW-FROM http://localhost:8020");
      headers.add("Access-Control-Allow-Origin", "*");  // 모든 도메인 허용
      headers.add("Access-Control-Allow-Methods", "GET, OPTIONS");
      headers.add("Access-Control-Allow-Headers", "Content-Type, Authorization");

      log.info("PDF 응답 완료 - 파일명: {}, 크기: {} bytes", pdfFileName, file.length());

      return ResponseEntity.ok()
          .headers(headers)
          .contentLength(file.length())
          .body(resource);

    } catch (Exception e) {
      log.error("서버 내부 오류 발생: appnum={}, message={}", appnum, e.getMessage(), e);
      return ResponseEntity.internalServerError().build();
    }
  }

  //첨부파일
  @RequestMapping(value = "/pdf2", method = RequestMethod.GET)
  public ResponseEntity<Resource> getPdf2(@RequestParam("appnum") String appnum) {
    try {
      log.info("PDF 조회 요청: appnum={}", appnum);

      // DB에서 PDF 파일명 조회
      Optional<String> optionalPdfFileName = paymentDetailService.findPdfFilenameByRealId2(appnum);
      if (optionalPdfFileName.isEmpty()) {
        log.warn("PDF 파일명을 찾을 수 없음: appnum={}", appnum);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
      }

      // 파일명 그대로 사용
      String pdfFileName = optionalPdfFileName.get();
      log.info("사용 파일명: {}", pdfFileName);

      // 운영체제별 저장 경로 설정
      String osName = System.getProperty("os.name").toLowerCase();
      String uploadDir = osName.contains("win") ? "C:\\Temp\\APP\\S_KRU\\"
          : System.getProperty("user.home") + "/APP/S_KRU";

      // PDF 파일 경로 설정 및 존재 여부 확인
      Path pdfPath = Paths.get(uploadDir, pdfFileName);
      log.info("PDF 파일 경로: {}", pdfPath.toString());

      if (!Files.exists(pdfPath)) {
        log.warn("파일이 존재하지 않음: {}", pdfPath.toString());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
      }

      // 파일 정보 로깅
      File file = pdfPath.toFile();
      log.info("파일 존재 확인 완료 - 파일 크기: {} bytes", file.length());

      // PDF 파일을 Resource로 변환 후 응답
      Resource resource = new FileSystemResource(file);
      log.info("Resource 변환 완료, 파일 응답 준비 시작");

      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_PDF);
      headers.setContentDisposition(ContentDisposition.inline().filename(pdfFileName, StandardCharsets.UTF_8).build());

      // `X-Frame-Options` 제거 (필요한 경우 추가 가능)
      headers.add("X-Frame-Options", "ALLOW-FROM http://localhost:8020");
      headers.add("Access-Control-Allow-Origin", "*");  // 모든 도메인 허용
      headers.add("Access-Control-Allow-Methods", "GET, OPTIONS");
      headers.add("Access-Control-Allow-Headers", "Content-Type, Authorization");

      log.info("PDF 응답 완료 - 파일명: {}, 크기: {} bytes", pdfFileName, file.length());

      return ResponseEntity.ok()
          .headers(headers)
          .contentLength(file.length())
          .body(resource);

    } catch (Exception e) {
      log.error("서버 내부 오류 발생: appnum={}, message={}", appnum, e.getMessage(), e);
      return ResponseEntity.internalServerError().build();
    }
  }

  @PostMapping("/changeState")
  public AjaxResult ChangeState(){
    AjaxResult result = new AjaxResult();
    try{

    }catch (Exception e){

    }

    return result;
  }

  private boolean fileExistsInPdfTable(String appnum) {
    return tbAa010PdfRepository.existsBySpdateAndFilenameIsNotNull(appnum);
  }

  private boolean fileExistsInAtchTable(String appnum) {
    return tbAa010AtchRepository.existsBySpdateAndFilenameIsNotNull(appnum);
  }

  private Map<String, Object> createFileMapFromPdf(String appnum, String label) {
    var entity = tbAa010PdfRepository.findBySpdate(appnum);
    return Map.of(
        "filepath", entity.getFilepath(),
        "filesvnm", entity.getFilename(),
        "fileornm", label
    );
  }

  private Map<String, Object> createFileMapFromAtch(String appnum, String label) {
    var entity = tbAa010AtchRepository.findBySpdate(appnum);
    return Map.of(
        "filepath", entity.getFilepath(),
        "filesvnm", entity.getFilename(),
        "fileornm", label
    );
  }

  @PostMapping("/downloader")
  public ResponseEntity<?> downloadFile(@RequestBody List<Map<String, Object>> downloadList) throws IOException {

    // 파일 목록과 파일 이름을 담을 리스트 초기화
    List<File> filesToDownload = new ArrayList<>();
    List<String> fileNames = new ArrayList<>();

    // ZIP 파일 이름을 설정할 변수 초기화
    String tketcrdtm = null;
    String tketnm = null;

    // 파일을 메모리에 쓰기
    for (Map<String, Object> fileInfo : downloadList) {
      String filePath = (String) fileInfo.get("filepath");    // 파일 경로
      String fileName = (String) fileInfo.get("filesvnm");    // 파일 이름(uuid)
      String originFileName = (String) fileInfo.get("fileornm");  //파일 원본이름(origin Name)

      File file = new File(filePath + File.separator + fileName);

      // 파일이 실제로 존재하는지 확인
      if (file.exists()) {
        filesToDownload.add(file);
        fileNames.add(originFileName); // 다운로드 받을 파일 이름을 originFileName으로 설정
      }
    }

    // 파일이 없는 경우
    if (filesToDownload.isEmpty()) {
      return ResponseEntity.notFound().build();
    }

    // 파일이 하나인 경우 그 파일을 바로 다운로드
    if (filesToDownload.size() == 1) {
      File file = filesToDownload.get(0);
      String originFileName = fileNames.get(0); // originFileName 가져오기

      HttpHeaders headers = new HttpHeaders();
      String encodedFileName = URLEncoder.encode(originFileName, StandardCharsets.UTF_8.toString()).replaceAll("\\+", "%20");
      headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=*''" + encodedFileName);
      headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
      headers.setContentLength(file.length());

      ByteArrayResource resource = new ByteArrayResource(Files.readAllBytes(file.toPath()));

      return ResponseEntity.ok()
          .headers(headers)
          .body(resource);
    }

    String zipFileName = (tketcrdtm != null && tketnm != null) ? tketcrdtm + "_" + tketnm + ".zip" : "download.zip";

    // 파일이 두 개 이상인 경우 ZIP 파일로 묶어서 다운로드
    ByteArrayOutputStream zipBaos = new ByteArrayOutputStream();
    try (ZipOutputStream zipOut = new ZipOutputStream(zipBaos)) {

      Set<String> addedFileNames = new HashSet<>(); // 이미 추가된 파일 이름을 저장할 Set
      int fileCount = 1;

      for (int i = 0; i < filesToDownload.size(); i++) {
        File file = filesToDownload.get(i);
        String originFileName = fileNames.get(i); // originFileName 가져오기

        // 파일 이름이 중복될 경우 숫자를 붙여 고유한 이름으로 만듦
        String uniqueFileName = originFileName;
        while (addedFileNames.contains(uniqueFileName)) {
          uniqueFileName = originFileName.replace(".", "_" + fileCount++ + ".");
        }

        // 고유한 파일 이름을 Set에 추가
        addedFileNames.add(uniqueFileName);

        try (FileInputStream fis = new FileInputStream(file)) {
          ZipEntry zipEntry = new ZipEntry(originFileName);
          zipOut.putNextEntry(zipEntry);

          byte[] buffer = new byte[1024];
          int len;
          while ((len = fis.read(buffer)) > 0) {
            zipOut.write(buffer, 0, len);
          }

          zipOut.closeEntry();
        } catch (IOException e) {
          e.printStackTrace();
          return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
      }

      zipOut.finish();
    } catch (IOException e) {
      e.printStackTrace();
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }

    ByteArrayResource zipResource = new ByteArrayResource(zipBaos.toByteArray());

    HttpHeaders headers = new HttpHeaders();
    String encodedZipFileName = URLEncoder.encode(zipFileName, StandardCharsets.UTF_8.toString()).replaceAll("\\+", "%20");
    headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=*''" + encodedZipFileName);
    headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
    headers.setContentLength(zipResource.contentLength());

    return ResponseEntity.ok()
        .headers(headers)
        .body(zipResource);
  }

}
