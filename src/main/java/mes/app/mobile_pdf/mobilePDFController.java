package mes.app.mobile_pdf;

import lombok.extern.slf4j.Slf4j;
import mes.app.mobile_pdf.Service.PDFService;
import mes.domain.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/mobilePDF")
public class mobilePDFController {


  @Autowired
  PDFService pdfService;

  // PDF 파일 조회 API
  @RequestMapping(value = "/pdf", method = RequestMethod.GET)
  public ResponseEntity<Resource> getPdf(@RequestParam("appnum") String appnum,
                                         Authentication auth) {
    try {
      log.info("PDF 조회 요청: appnum={}", appnum);
      User user = (User) auth.getPrincipal();
      String perId = user.getAgencycd().replaceFirst("^p", "");

      // DB에서 PDF 파일 정보 조회
      Optional<PDFService.PdfFileInfo> optionalPdfFileInfo = pdfService.findPdfFileInfoByAppgubun(appnum, perId);
      if (optionalPdfFileInfo.isEmpty()) {
        log.warn("PDF 파일을 찾을 수 없음: appnum={}", appnum);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
      }

      // PdfFileInfo에서 경로와 파일명 추출
      PDFService.PdfFileInfo pdfFileInfo = optionalPdfFileInfo.get();
      String pdfFilePath = pdfFileInfo.getFilePath();
      String pdfFileName = pdfFileInfo.getFileName();
      log.info("사용 파일명: {}, 파일 경로: {}", pdfFileName, pdfFilePath);

      // PDF 파일 경로 확인 및 존재 여부 체크
      Path pdfPath = Paths.get(pdfFilePath); // 전체 경로를 사용

      if (!Files.exists(pdfPath)) {
        log.warn("파일이 존재하지 않음: {}", pdfPath.toString());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
      }

      // PDF 파일을 Resource로 변환 후 응답
      File file = pdfPath.toFile();
      Resource resource = new FileSystemResource(file);

      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_PDF);
      headers.setContentDisposition(ContentDisposition.inline().filename(pdfFileName, StandardCharsets.UTF_8).build());

      // `X-Frame-Options` 제거 (필요한 경우 추가 가능)
      headers.add("X-Frame-Options", "ALLOW-FROM http://localhost:8040");
      headers.add("Access-Control-Allow-Origin", "*");  // 모든 도메인 허용
      headers.add("Access-Control-Allow-Methods", "GET, OPTIONS");
      headers.add("Access-Control-Allow-Headers", "Content-Type, Authorization");

      return ResponseEntity.ok()
          .headers(headers)
          .contentLength(file.length())
          .body(resource);

    } catch (Exception e) {
      log.error("서버 내부 오류 발생", e);
      return ResponseEntity.internalServerError().build();
    }
  }

  //pdf 다운로드
  @RequestMapping(value = "/pdfDownload", method = RequestMethod.GET)
  public ResponseEntity<Resource> downloadPdf(@RequestParam("appnum") String appnum,
                                              Authentication auth) {
    try {
      log.info("📄 PDF 다운로드 요청: appnum={}", appnum);

      User user = (User) auth.getPrincipal();
      String perId = user.getAgencycd().replaceFirst("^p", "");

      // DB에서 PDF 파일 정보 조회
      Optional<PDFService.PdfFileInfo> optionalPdfFileInfo = pdfService.findPdfFileInfoByAppgubun(appnum, perId);
      if (optionalPdfFileInfo.isEmpty()) {
        log.warn("PDF 파일을 찾을 수 없음: appnum={}", appnum);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
      }

      PDFService.PdfFileInfo pdfFileInfo = optionalPdfFileInfo.get();
      String pdfFilePath = pdfFileInfo.getFilePath();
      String pdfFileName = pdfFileInfo.getFileName();

      log.info("📎 다운로드 대상 파일명: {}, 경로: {}", pdfFileName, pdfFilePath);

      Path path = Paths.get(pdfFilePath).normalize();
      if (!Files.exists(path)) {
        log.warn("파일이 존재하지 않음: {}", path);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
      }

      File file = path.toFile();
      Resource resource = new FileSystemResource(file);

      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_PDF);
      headers.setContentDisposition(
          ContentDisposition.attachment()
              .filename(pdfFileName, StandardCharsets.UTF_8)
              .build()
      );

      headers.add("Access-Control-Allow-Origin", "*");
      headers.add("Access-Control-Allow-Methods", "GET, OPTIONS");
      headers.add("Access-Control-Allow-Headers", "Content-Type, Authorization");

      return ResponseEntity.ok()
          .headers(headers)
          .contentLength(file.length())
          .body(resource);

    } catch (Exception e) {
      log.error("❗ PDF 다운로드 처리 중 서버 오류 발생", e);
      return ResponseEntity.internalServerError().build();
    }
  }
}
