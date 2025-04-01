package mes.app.PaymentList;

import lombok.extern.slf4j.Slf4j;
import mes.app.PaymentList.service.PdfService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

@Slf4j
@RestController
@RequestMapping("/appkey")
public class FileMkController {

  @Autowired
  private PdfService pdfService;

  /*@GetMapping
  public ResponseEntity<String> generatePdf(@RequestParam String key) {
    try {
      log.info("🔹 요청 수신: key={}", key);

      byte[] pdfData;
      String custcd;
      String filename;

      // 📌 key 값이 "A"로 시작하는 경우 별도 테이블에서 조회
      if (key.startsWith("A")) {
        log.info("🔹 A로 시작하는 key 감지, 다른 테이블에서 조회: key={}", key);
        pdfData = pdfService.getPdfByKeyForA(key);              // PDF 데이터
        custcd = pdfService.getCustcdBySpdateForA(key);         // 고객 코드
        filename = pdfService.getFilenameByKeyForA(key);        // 파일명
      } else {
        log.info("🔹 일반 key 처리 진행: key={}", key);
        pdfData = pdfService.getPdfByKey(key);
        custcd = pdfService.getCustcdBySpdate(key);
        filename = pdfService.getFilenameByKey(key);
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

      // 📌 파일명 유효성 및 확장자 처리
      if (filename == null || filename.trim().isEmpty()) {
        filename = key + ".pdf"; // fallback
        log.warn("⚠️ DB에서 파일명을 찾지 못해 기본 파일명으로 대체: {}", filename);
      } else if (!filename.toLowerCase().endsWith(".pdf")) {
        filename += ".pdf";
        log.info("📎 확장자 추가된 파일명: {}", filename);
      }

      // 📌 PDF 저장 경로 설정
      String directoryPath = "C:/temp/APP/" + custcd + "/";
      String filePath = directoryPath + filename;
      log.info("📂 파일 저장 경로 설정: {}", filePath);

      // 📁 디렉토리 생성
      File directory = new File(directoryPath);
      if (!directory.exists()) {
        directory.mkdirs();
        log.info("📁 디렉토리 생성 완료: {}", directoryPath);
      }

      // 🗑 기존 파일 삭제
      File file = new File(filePath);
      if (file.exists()) {
        file.delete();
        log.info("🗑 기존 파일 삭제 완료: {}", filePath);
      }

      // 💾 파일 저장
      Files.write(Paths.get(filePath), pdfData);
      log.info("✅ PDF 파일 저장 완료: {}", filePath);

      // 📝 파일 경로 DB 업데이트
      boolean isUpdated = pdfService.updateFilePath(key, filePath);
      if (!isUpdated) {
        log.warn("⚠️ 파일 경로 DB 업데이트 실패: key={}, filePath={}", key, filePath);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("파일은 저장되었지만 경로 업데이트에 실패했습니다.");
      }
      log.info("✅ 파일 경로 DB 업데이트 성공: key={}, filePath={}", key, filePath);

      return ResponseEntity.ok("PDF 파일 생성 및 경로 업데이트 완료: " + filePath);

    } catch (Exception e) {
      log.error("🚨 파일 생성 중 오류 발생: key={}, error={}", key, e.getMessage(), e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("파일 생성 중 오류 발생: " + e.getMessage());
    }
  }*/
  @GetMapping
  public ResponseEntity<String> generatePdf(@RequestParam String key) {
    try {
      log.info("🔹 요청 수신: key={}", key);

      byte[] pdfData;
      String custcd;
      String filename;

      if (key.startsWith("A")) {
        log.info("🔹 A로 시작하는 key 감지, 다른 테이블에서 조회: key={}", key);

        // 1차: A 테이블 처리
        pdfData = pdfService.getPdfByKeyForA(key);
        custcd = pdfService.getCustcdBySpdateForA(key);
        filename = pdfService.getFilenameByKeyForA(key);

        if (pdfData != null) {
          processPdfFile(key, pdfData, custcd, filename); // 🔁 공통 로직 추출하여 처리
        } else {
          log.warn("❌ A 테이블에서 PDF 데이터 없음, 일반 key 처리 시도 예정: key={}", key);
        }

        // 2차: A 제거 후 일반 테이블 처리
        String trimmedKey = key.substring(1);
        log.info("🔄 일반 테이블 재조회 시작 (A 제거된 key): {}", trimmedKey);
        pdfData = pdfService.getPdfByKey(trimmedKey);
        custcd = pdfService.getCustcdBySpdate(trimmedKey);
        filename = pdfService.getFilenameByKey(trimmedKey);
        key = trimmedKey; // 이후 처리에서는 trimmedKey로 간주
      } else {
        // 일반 처리
        log.info("🔹 일반 key 처리 진행: key={}", key);
        pdfData = pdfService.getPdfByKey(key);
        custcd = pdfService.getCustcdBySpdate(key);
        filename = pdfService.getFilenameByKey(key);
      }

      // 데이터 체크
      if (pdfData == null) {
        log.warn("❌ PDF 데이터 없음: key={}", key);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("PDF 데이터를 찾을 수 없습니다.");
      }
      if (custcd == null || custcd.isEmpty()) {
        log.warn("❌ 고객 코드 없음: key={}", key);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("고객 코드를 찾을 수 없습니다.");
      }

      // 공통 처리
      String filePath = processPdfFile(key, pdfData, custcd, filename);

      // DB 업데이트
      boolean isUpdated = pdfService.updateFilePath(key, filePath);
      if (!isUpdated) {
        log.warn("⚠️ 경로 업데이트 실패: {}", filePath);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("파일 저장 완료, 경로 업데이트 실패");
      }

      return ResponseEntity.ok("PDF 파일 생성 및 경로 업데이트 완료: " + filePath);

    } catch (Exception e) {
      log.error("🚨 오류 발생: key={}, error={}", key, e.getMessage(), e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("오류 발생: " + e.getMessage());
    }
  }

  // 🔁 공통 파일 처리 메서드 추출
  private String processPdfFile(String key, byte[] pdfData, String custcd, String filename) throws IOException {
    if (filename == null || filename.trim().isEmpty()) {
      filename = key + ".pdf";
      log.warn("⚠️ 파일명 없음 -> 기본 파일명 사용: {}", filename);
    } else if (!filename.toLowerCase().endsWith(".pdf")) {
      filename += ".pdf";
      log.info("📎 확장자 추가: {}", filename);
    }

    String directoryPath = "C:/temp/APP/" + custcd + "/";
    String filePath = directoryPath + filename;
    log.info("📂 저장 경로: {}", filePath);

    File directory = new File(directoryPath);
    if (!directory.exists()) {
      directory.mkdirs();
      log.info("📁 디렉토리 생성 완료: {}", directoryPath);
    }

    File file = new File(filePath);
    if (file.exists()) {
      file.delete();
      log.info("🗑 기존 파일 삭제 완료: {}", filePath);
    }

    Files.write(Paths.get(filePath), pdfData);
    log.info("✅ PDF 저장 완료: {}", filePath);

    return filePath;
  }

}
