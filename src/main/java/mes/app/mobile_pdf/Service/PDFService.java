package mes.app.mobile_pdf.Service;

import lombok.extern.slf4j.Slf4j;
import mes.domain.services.SqlRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;


@Slf4j
@Service
public class PDFService {

  @Autowired
  SqlRunner sqlRunner;

  public Optional<PdfFileInfo> findPdfFileInfoByAppgubun(String appnum, String perId) {
    MapSqlParameterSource params = new MapSqlParameterSource();
    params.addValue("appnum", appnum);
    params.addValue("perid", perId);

    String sql = "SELECT e.*, " +
        "aa.filepath AS atch_filepath, aa.filename AS atch_filename, " +
        "tap.filepath AS pdf_filepath, tap.filename AS pdf_filename " +
        "FROM tb_e080 e " +
        "LEFT JOIN TB_AA010ATCH aa ON aa.spdate = e.appnum " +
        "LEFT JOIN TB_AA010PDF tap ON tap.spdate = e.appnum " +
        "WHERE e.appnum = :appnum AND e.appperid = :perid;";

    try {
      log.info("pdf조회 SQL: {}", sql);
      log.info("SQL Parameters: {}", params.getValues());

      List<Map<String, Object>> result = sqlRunner.getRows(sql, params);

      if (!result.isEmpty()) {
        Map<String, Object> row = result.get(0);
        String filepath = (String) row.get("pdf_filepath");
        String filename = (String) row.get("pdf_filename");

        log.info("조회된 filepath: {}, filename: {}", filepath, filename);

        if (filepath != null && filename != null) {
          return Optional.of(new PdfFileInfo(filepath, filename));
        } else {
          log.warn("PDF 경로나 파일명이 null입니다. filepath={}, filename={}", filepath, filename);
        }
      } else {
        log.warn("PDF 정보가 없습니다. appnum={}, perid={}", appnum, perId);
      }
    } catch (Exception e) {
      log.error("PDF 파일 정보를 조회하는 중 오류 발생", e);
    }

    return Optional.empty();
  }


  public class PdfFileInfo {
    private final String filePath;
    private final String fileName;

    public PdfFileInfo(String filePath, String fileName) {
      this.filePath = filePath;
      this.fileName = fileName;
    }

    public String getFilePath() {
      return filePath;
    }

    public String getFileName() {
      return fileName;
    }
  }


}
