package mes.app.PaymentList;

import lombok.extern.slf4j.Slf4j;
import mes.app.PaymentList.service.PdfService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

@Slf4j
@RestController
@RequestMapping("/appkey")
public class FileMkController {

  @Autowired
  private PdfService pdfService;

  @GetMapping
  public ResponseEntity<String> generatePdf(@RequestParam String key) {
    try {
      log.info("🔹 요청 수신: key={}", key);

      byte[] pdfData;
      String custcd;

      // 📌 key 값이 "A"로 시작하는 경우 별도 테이블에서 조회
      if (key.startsWith("A")) {
        log.info("🔹 A로 시작하는 key 감지, 다른 테이블에서 조회: key={}", key);
        pdfData = pdfService.getPdfByKeyForA(key);  // 다른 테이블에서 조회
        custcd = pdfService.getCustcdBySpdateForA(key);  // 다른 테이블에서 조회
      } else {
        log.info("🔹 일반 key 처리 진행: key={}", key);
        pdfData = pdfService.getPdfByKey(key);  // 기존 테이블에서 조회
        custcd = pdfService.getCustcdBySpdate(key);  // 기존 테이블에서 조회
      }

      // 📌 데이터 존재 여부 확인
      if (pdfData == null) {
        log.warn("❌ 파일을 찾을 수 없음: key={}", key);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("파일을 찾을 수 없습니다.");
      }
      log.info("✅ PDF 데이터 조회 성공: key={}", key);

      if (custcd == null || custcd.isEmpty()) {
        log.warn("❌ 고객 코드(custcd)를 찾을 수 없음: key={}", key);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("고객 코드(custcd)를 찾을 수 없습니다.");
      }
      log.info("✅ 고객 코드 조회 성공: key={}, custcd={}", key, custcd);

      // 📌 PDF 저장 경로 설정
      String directoryPath = "C:/temp/APP/" + custcd + "/";
      String filePath = directoryPath + key + ".pdf";
      log.info("📂 파일 저장 경로 설정: {}", filePath);

      // 📌 디렉토리 확인 후 없으면 생성
      File directory = new File(directoryPath);
      if (!directory.exists()) {
        directory.mkdirs();
        log.info("📁 디렉토리 생성 완료: {}", directoryPath);
      }

      // 📌 기존 파일이 존재하면 삭제 후 새 파일 저장
      File file = new File(filePath);
      if (file.exists()) {
        file.delete();
        log.info("🗑 기존 파일 삭제 완료: {}", filePath);
      }

      // 📌 파일 저장
      Files.write(Paths.get(filePath), pdfData);
      log.info("✅ PDF 파일 저장 완료: {}", filePath);

      return ResponseEntity.ok("PDF 파일 생성 완료: " + filePath);
    } catch (Exception e) {
      log.error("🚨 파일 생성 중 오류 발생: key={}, error={}", key, e.getMessage(), e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("파일 생성 중 오류 발생: " + e.getMessage());
    }
  }



}
