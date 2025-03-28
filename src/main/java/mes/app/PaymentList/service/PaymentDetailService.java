package mes.app.PaymentList.service;

import lombok.extern.slf4j.Slf4j;
import mes.domain.services.SqlRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
public class PaymentDetailService {

  @Autowired
  SqlRunner sqlRunner;

  public List<Map<String, Object>> getPaymentList(String spjangcd, String startDate, String endDate, String searchPayment, String searchUserNm, String agencycd) {
    MapSqlParameterSource params = new MapSqlParameterSource();
    params.addValue("as_spjangcd", spjangcd);
    params.addValue("agencycd", agencycd);
    StringBuilder sql = new StringBuilder("""
        SELECT e080.repodate,
            e080.repoperid,
            (select pernm from tb_ja001 where perid = 'p' + repoperid) as repopernm,
            e080.appgubun,
            ca510.com_code AS papercd,
            ca510.com_cnam AS papercd_name,
            uc.Value AS appgubun_display,
            e080.appdate,
            e080.appnum,
            e080.appperid,
            e080.title,
            e080.remark,
            CASE
               WHEN EXISTS (
                   SELECT 1 FROM TB_AA010ATCH
                   WHERE spdate = 'A' + e080.appnum OR spdate = 'AS' + e080.appnum
               ) THEN (
                   SELECT TOP 1 CONCAT(spdate, '|', filename, '|', filepath)
                   FROM TB_AA010ATCH
                   WHERE spdate = 'A' + e080.appnum OR spdate = 'AS' + e080.appnum
               )
               ELSE (
                   SELECT TOP 1 CONCAT(spdate, '|', filename, '|', filepath)
                   FROM TB_AA010PDF
                   WHERE spdate = e080.appnum
               )
           END AS file_info
        FROM tb_e080 e080 WITH(NOLOCK)
       LEFT JOIN user_code uc ON uc.Code = e080.appgubun
       LEFT JOIN tb_ca510 ca510
           ON ca510.com_cls = '620'
          AND ca510.com_code = e080.papercd
        WHERE spjangcd = :as_spjangcd
        AND appperid = :agencycd
        AND flag = '1'
        """);

    // startDate 필터링
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
    String startDateFormatted = LocalDate.parse(startDate).format(formatter);
    sql.append(" AND repodate >= :as_stdate ");
    params.addValue("as_stdate", startDateFormatted);


    // endDate 필터링
    if (endDate != null && !endDate.isEmpty()) {
      sql.append(" AND repodate <= :as_enddate ");
      params.addValue("as_enddate", endDate);
    }

    // 검색 조건 추가
    if (searchUserNm != null && !searchUserNm.isEmpty()) {
      sql.append(" AND appperid LIKE :searchUserNm ");
      params.addValue("searchUserNm", "%" + searchUserNm + "%");
    }

    if (searchPayment == null || searchPayment.equals("all") || searchPayment.isEmpty()) {
      sql.append(" AND (appgubun LIKE '%' OR :as_appgubun = '%') "); // 모든 값 허용
      params.addValue("as_appgubun", "%");
    } else {
      sql.append(" AND appgubun = :as_appgubun ");
      params.addValue("as_appgubun", searchPayment);
    }

//    log.info("결재내역 List SQL: {}", sql);
//    log.info("SQL Parameters: {}", params.getValues());
    return sqlRunner.getRows(sql.toString(), params);
  }

  public List<Map<String, Object>> getPaymentList1(String spjangcd, String startDate, String endDate, String agencycd) {
    MapSqlParameterSource params = new MapSqlParameterSource();

    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
    String startDateFormatted = LocalDate.parse(startDate).format(formatter);
    String endDateFormatted = LocalDate.parse(endDate).format(formatter);

    params.addValue("as_stdate", startDateFormatted);
    params.addValue("as_enddate", endDateFormatted);
    params.addValue("as_spjangcd", spjangcd);
    params.addValue("as_perid", agencycd);
    StringBuilder sql = new StringBuilder("""
        SELECT (select count(appgubun) from tb_e080 WITH(NOLOCK) where appgubun = '001' AND appperid = :as_perid AND flag = '1' AND repodate Between :as_stdate AND :as_enddate and spjangcd = :as_spjangcd ) as appgubun1,
        	    (select count(appgubun) from tb_e080 WITH(NOLOCK) where appgubun = '101' AND appperid = :as_perid AND flag = '1'  AND repodate Between :as_stdate AND :as_enddate) as appgubun2,
        	    (select count(appgubun) from tb_e080 WITH(NOLOCK) where appgubun = '131' AND appperid = :as_perid AND flag = '1'  AND repodate Between :as_stdate AND :as_enddate) as appgubun3,
        	    (select count(appgubun) from tb_e080 WITH(NOLOCK) where appgubun = '201' AND appperid = :as_perid AND flag = '1'  AND repodate Between :as_stdate AND :as_enddate) as appgubun4
        FROM dual
        """);
//    log.info("결재목록_문서현황 List SQL: {}", sql);
//    log.info("SQL Parameters: {}", params.getValues());
    return sqlRunner.getRows(sql.toString(), params);
  }

  public Optional<String> findPdfFilenameByRealId(String appnum) {
    MapSqlParameterSource params = new MapSqlParameterSource();
    params.addValue("appnum", appnum);

    String sql = "select filename from TB_AA010PDF where spdate = :appnum;";

    try {
      // SQL 실행 후 결과 조회
//      log.info("결재승인PDF 파일 찾기 SQL: {}", sql);
//      log.info("SQL Parameters: {}", params.getValues());
      List<Map<String, Object>> result = sqlRunner.getRows(sql, params);

      if (!result.isEmpty() && result.get(0).get("filename") != null) {
        return Optional.of((String) result.get(0).get("filename"));
      }
    } catch (Exception e) {
      log.info("PDF 파일명을 조회하는 중 오류 발생: {}", e.getMessage(), e);
    }

    return Optional.empty(); // 결과가 없으면 빈 Optional 반환
  }

  public Optional<String> findPdfFilenameByRealId2(String appnum) {
    MapSqlParameterSource params = new MapSqlParameterSource();
    params.addValue("appnum", appnum);

    String sql = "select filename from TB_AA010ATCH WHERE spdate = 'A' + :appnum;";

    try {
      // SQL 실행 후 결과 조회
//      log.info("첨부파일 PDF 파일 찾기 SQL: {}", sql);
//      log.info("SQL Parameters: {}", params.getValues());
      List<Map<String, Object>> result = sqlRunner.getRows(sql, params);

      if (!result.isEmpty() && result.get(0).get("filename") != null) {
        return Optional.of((String) result.get(0).get("filename"));
      }
    } catch (Exception e) {
      log.info("첨부파일 PDF 파일명을 조회하는 중 오류 발생: {}", e.getMessage(), e);
    }

    return Optional.empty(); // 결과가 없으면 빈 Optional 반환
  }

  // 지출결의서 (TB_AA007, TB_E080)
  public boolean updateStateForS(String appnum, String appgubun, String stateCode, String remark, String currentAppperid, String papercd) {
    MapSqlParameterSource params = new MapSqlParameterSource();
    params.addValue("appnum", appnum);
    
    // Step 1: TB_E080 결재라인 전체 조회
    String TB_E080Sql = """
    SELECT COUNT(*) AS cnt
    FROM TB_E080
    WHERE appnum = :appnum
      AND seq > (
        SELECT seq
        FROM TB_E080
        WHERE appnum = :appnum
          AND appperid = :currentAppperid
      )
      AND appgubun = '101'
""";

    params.addValue("appnum", appnum);
    params.addValue("currentAppperid", currentAppperid);

    Map<String, Object> row = sqlRunner.getRow(TB_E080Sql, params);
    int count = row.get("cnt") != null ? ((Number) row.get("cnt")).intValue() : 0;

    // 상태 제한 처리
    if (count > 0 && !"101".equals(stateCode)) {
      log.warn("❌ 내 뒤에 있는 사람이 이미 승인함 → 승인 외 상태 변경 불가 (요청: {})", stateCode);
      return false;
    }

    log.info("✅ 상태 변경 가능: stateCode={}, 뒤에 승인자 수={}", stateCode, count);

    // Step 2: TB_AA007 문서 조회
    String aa007Sql = """
      SELECT *
      FROM TB_AA007
      WHERE appnum = :appnum
         OR 'S' + spdate + spnum + spjangcd = :appnum
  """;
    List<Map<String, Object>> aa007Rows = sqlRunner.getRows(aa007Sql, params);

    if (aa007Rows != null && !aa007Rows.isEmpty()) {
      log.info("✅ TB_AA007 문서 찾음: appnum={}", appnum);

      // TB_AA007 업데이트
      String updateAa007Sql = """
        UPDATE TB_AA007
        SET appgubun = :action,
            remark = :remark,
            inputdate = GETDATE()
        WHERE appnum = :appnum
           OR 'S' + spdate + spnum + spjangcd = :appnum
    """;

      params.addValue("action", stateCode);
      params.addValue("remark", remark);
      int aa007Affected = sqlRunner.execute(updateAa007Sql, params);
      log.info("📝 TB_AA007 업데이트 완료: 변경된 row 수 = {}", aa007Affected);
    } else {
      log.warn("❌ TB_AA007에서 문서 찾지 못함: appnum={}", appnum);
      return false;
    }

    // Step 3: TB_E080 업데이트 (현재 결재자만 대상)
    String updateE080Sql = """
      UPDATE TB_E080
      SET appgubun = :action,
          remark = :remark,
        appdate = CONVERT(varchar(8), GETDATE(), 112)
      WHERE appnum = :appnum
        AND appperid = :currentAppperid
        AND papercd = :papercd
  """;

    params.addValue("currentAppperid", currentAppperid);
    params.addValue("papercd", String.valueOf(papercd));
    int e080Affected = sqlRunner.execute(updateE080Sql, params);
    log.info("📝 TB_E080 업데이트 완료: 변경된 row 수 = {}", e080Affected);

    return e080Affected > 0;
  }

  // 전표문서 (TB_AA009, TB_E080)
  public boolean updateStateForNumberZZ(String appnum, String appgubun, String stateCode, String remark, String currentAppperid, String papercd) {
    MapSqlParameterSource params = new MapSqlParameterSource();
    params.addValue("appnum", appnum);

    // Step 1: TB_E080 결재라인 전체 조회
    String TB_E080Sql = """
    SELECT COUNT(*) AS cnt
    FROM TB_E080
    WHERE appnum = :appnum
      AND seq > (
        SELECT seq
        FROM TB_E080
        WHERE appnum = :appnum
          AND appperid = :currentAppperid
      )
      AND appgubun = '101'
""";

    params.addValue("appnum", appnum);
    params.addValue("currentAppperid", currentAppperid);

    Map<String, Object> row = sqlRunner.getRow(TB_E080Sql, params);
    int count = row.get("cnt") != null ? ((Number) row.get("cnt")).intValue() : 0;

    // 상태 제한 처리
    if (count > 0 && !"101".equals(stateCode)) {
      log.warn("❌ 내 뒤에 있는 사람이 이미 승인함 → 승인 외 상태 변경 불가 (요청: {})", stateCode);
      return false;
    }

    log.info("✅ 상태 변경 가능: stateCode={}, 뒤에 승인자 수={}", stateCode, count);
    // Step 2: TB_AA009 문서 조회
    String aa009Sql = """
     SELECT * FROM TB_AA009
       WHERE appnum = :appnum
          OR spdate  + spnum + SPJANGCD = :appnum;
  """;
    List<Map<String, Object>> AA009Rows = sqlRunner.getRows(aa009Sql, params);

    if (AA009Rows != null && !AA009Rows.isEmpty()) {
      log.info("✅ TB_AA009 문서 찾음: appnum={}", appnum);

      // TB_AA009 업데이트
      String updateAa009Sql = """
        UPDATE TB_AA009
        SET appgubun = :action,
            remark = :remark,
            inputdate = GETDATE()
        WHERE appnum = :appnum
           OR spdate  + spnum + SPJANGCD = :appnum;
    """;

      params.addValue("action", stateCode);
      params.addValue("remark", remark);
      int aa009Affected = sqlRunner.execute(updateAa009Sql, params);
      log.info("📝 TB_AA009 업데이트 완료: 변경된 row 수 = {}", aa009Affected);
    } else {
      log.warn("❌ TB_AA009 문서 찾지 못함: appnum={}", appnum);
      return false;
    }

    // Step 3: TB_E080 업데이트 (현재 결재자만 대상)
    String updateE080Sql = """
      UPDATE TB_E080
      SET appgubun = :action,
          remark = :remark,
         appdate = CONVERT(varchar(8), GETDATE(), 112)
      WHERE appnum = :appnum
        AND appperid = :currentAppperid
        AND papercd = :papercd
  """;

    params.addValue("currentAppperid", currentAppperid);
    params.addValue("papercd", String.valueOf(papercd));
    int e080Affected = sqlRunner.execute(updateE080Sql, params);
    log.info("📝 TB_E080 업데이트 완료: 변경된 row 수 = {}", e080Affected);

    return e080Affected > 0;

  }

  // 휴가 문서 상태 변경 (TB_PB204, TB_E080)
  public boolean updateStateForV(String appnum, String appgubun, String stateCode, String remark, String currentAppperid, String papercd) {
    MapSqlParameterSource params = new MapSqlParameterSource();
    params.addValue("appnum", appnum);

    // Step 1: TB_E080 결재라인 전체 조회
    String TB_E080Sql = """
    SELECT COUNT(*) AS cnt
    FROM TB_E080
    WHERE appnum = :appnum
      AND seq > (
        SELECT seq
        FROM TB_E080
        WHERE appnum = :appnum
          AND appperid = :currentAppperid
      )
      AND appgubun = '101'
""";

    params.addValue("appnum", appnum);
    params.addValue("currentAppperid", currentAppperid);

    Map<String, Object> row = sqlRunner.getRow(TB_E080Sql, params);
    int count = row.get("cnt") != null ? ((Number) row.get("cnt")).intValue() : 0;

  // 상태 제한 처리
    if (count > 0 && !"101".equals(stateCode)) {
      log.warn("❌ 내 뒤에 있는 사람이 이미 승인함 → 승인 외 상태 변경 불가 (요청: {})", stateCode);
      return false;
    }

    log.info("✅ 상태 변경 가능: stateCode={}, 뒤에 승인자 수={}", stateCode, count);

    // Step 2: TB_PB204 문서 조회
    String PB204Sql = """
      SELECT * FROM TB_PB204
      WHERE appnum = :appnum
        OR 'V' + VAYEAR + VANUM + SPJANGCD = :appnum;
  """;
    List<Map<String, Object>> TB_PB204Rows = sqlRunner.getRows(PB204Sql, params);

    if (TB_PB204Rows != null && !TB_PB204Rows.isEmpty()) {
      log.info("✅ TB_PB204 문서 찾음: appnum={}", appnum);

      // TB_PB204 업데이트
      String updatePB204SqlSql = """
        UPDATE TB_PB204
        SET appgubun = :action,
            remark = :remark,
            appdate = CONVERT(varchar(8), GETDATE(), 112)
        WHERE appnum = :appnum
            OR 'V' + VAYEAR + VANUM + SPJANGCD = :appnum
    """;

      params.addValue("action", stateCode);
      params.addValue("remark", remark);
      int TB_PB204ffected = sqlRunner.execute(updatePB204SqlSql, params);
      log.info("📝 TB_PB204 업데이트 완료: 변경된 row 수 = {}", TB_PB204ffected);
    } else {
      log.warn("❌ TB_PB204에서 문서 찾지 못함: appnum={}", appnum);
      return false;
    }

    // Step 3: TB_E080 업데이트 (현재 결재자만 대상)
    String updateE080Sql = """
      UPDATE TB_E080
      SET appgubun = :action,
          remark = :remark,
        appdate = CONVERT(varchar(8), GETDATE(), 112)
      WHERE appnum = :appnum
        AND appperid = :currentAppperid
        AND papercd = :papercd
  """;

    params.addValue("currentAppperid", currentAppperid);
    params.addValue("papercd", String.valueOf(papercd));
    int e080Affected = sqlRunner.execute(updateE080Sql, params);
    log.info("📝 TB_E080 업데이트 완료: 변경된 row 수 = {}", e080Affected);

    return e080Affected > 0;
  }


  public boolean canCancelApproval(String appnum, String appperid) {
    MapSqlParameterSource params = new MapSqlParameterSource();
    params.addValue("appnum", appnum);
    params.addValue("appperid", appperid);

    // 로그인한 사람의 seq 찾기
    String seqSql = """
        SELECT seq
        FROM TB_E080
        WHERE appnum = :appnum AND appperid = :appperid
    """;

    Integer mySeq = sqlRunner.queryForObject(seqSql, params, (rs, rowNum) -> rs.getInt(1));
    if (mySeq == null) return false;

    // 자신보다 높은 seq가 결재했는지 체크
    String checkSql = """
        SELECT COUNT(1)
        FROM TB_E080
        WHERE appnum = :appnum
          AND seq > :mySeq
          AND appgubun <> '101'
    """;

    params.addValue("mySeq", mySeq);
    int count = sqlRunner.queryForCount(checkSql, params);

    if (count > 0) {
      return false;
    }

    // 나는 가장 마지막 순번인가?
    String maxSeqSql = """
        SELECT MAX(seq)
        FROM TB_E080
        WHERE appnum = :appnum
    """;

    int maxSeq = sqlRunner.queryForObject(maxSeqSql, params, (rs, rowNum) -> rs.getInt(1));

    return mySeq == maxSeq || count == 0;
  }

  public boolean isAlreadyApproved(String appnum, String appperid) {
    String sql = """
        SELECT COUNT(1)
        FROM TB_E080
        WHERE appnum = :appnum AND appperid = :appperid AND appdate IS NOT NULL
    """;
    MapSqlParameterSource params = new MapSqlParameterSource()
        .addValue("appnum", appnum)
        .addValue("appperid", appperid);
    return sqlRunner.queryForCount(sql, params) > 0;
  }

  public String getAgencyName() {
    String sql = "SELECT spjangnm FROM tb_xa012";
    MapSqlParameterSource param = new MapSqlParameterSource();
    Map<String, Object> row = sqlRunner.getRow(sql, param);

    return (row != null && row.get("spjangnm") != null)
        ? row.get("spjangnm").toString()
        : "기관명 없음";
  }

}
