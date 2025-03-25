package mes.app.PaymentList.service;

import lombok.extern.slf4j.Slf4j;
import mes.domain.repository.approval.TB_PB204Repository;
import mes.domain.repository.sportsRepository.E080Repository;
import mes.domain.services.SqlRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
public class PaymentDetailService {

  @Autowired
  SqlRunner sqlRunner;

  @Autowired
  TB_PB204Repository tbPb204Repository;

  @Autowired
  E080Repository e080Repository;

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
        left join user_code uc on uc.Code = e080.appgubun
        LEFT JOIN tb_ca510 ca510 ON ca510.com_cls = '620' AND ca510.com_code <> '00'
        WHERE spjangcd = :as_spjangcd
        AND appperid = :agencycd
        AND flag = '1'
        """);

    // startDate 필터링
    if (startDate != null && !startDate.isEmpty()) {
      sql.append(" AND repodate >= :as_stdate ");
      params.addValue("as_stdate", startDate);
    }

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

    log.info("결재내역 List SQL: {}", sql);
    log.info("SQL Parameters: {}", params.getValues());
    return sqlRunner.getRows(sql.toString(), params);
  }

  public List<Map<String, Object>> getPaymentList1(String spjangcd, String startDate, String endDate, String agencycd) {
    MapSqlParameterSource params = new MapSqlParameterSource();
    params.addValue("as_spjangcd", spjangcd);
    params.addValue("as_stdate", startDate);
    params.addValue("as_enddate", endDate);
    params.addValue("as_perid", agencycd);
    StringBuilder sql = new StringBuilder("""
        SELECT (select count(appgubun) from tb_e080 WITH(NOLOCK) where appgubun = '001' AND appperid = :as_perid AND flag = '1' AND repodate Between :as_stdate AND :as_enddate and spjangcd = :as_spjangcd ) as appgubun1,
        	    (select count(appgubun) from tb_e080 WITH(NOLOCK) where appgubun = '101' AND appperid = :as_perid AND flag = '1'  AND repodate Between :as_stdate AND :as_enddate) as appgubun2,
        	    (select count(appgubun) from tb_e080 WITH(NOLOCK) where appgubun = '131' AND appperid = :as_perid AND flag = '1'  AND repodate Between :as_stdate AND :as_enddate) as appgubun3,
        	    (select count(appgubun) from tb_e080 WITH(NOLOCK) where appgubun = '201' AND appperid = :as_perid AND flag = '1'  AND repodate Between :as_stdate AND :as_enddate) as appgubun4
        FROM dual
        """);
    log.info("결재목록_문서현황 List SQL: {}", sql);
    log.info("SQL Parameters: {}", params.getValues());
    return sqlRunner.getRows(sql.toString(), params);
  }

  public Optional<String> findPdfFilenameByRealId(String appnum) {
    MapSqlParameterSource params = new MapSqlParameterSource();
    params.addValue("appnum", appnum);

    String sql = "select filename from TB_AA010PDF where spdate = :appnum;";

    try {
      // SQL 실행 후 결과 조회
      log.info("결재승인PDF 파일 찾기 SQL: {}", sql);
      log.info("SQL Parameters: {}", params.getValues());
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
      log.info("첨부파일 PDF 파일 찾기 SQL: {}", sql);
      log.info("SQL Parameters: {}", params.getValues());
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
    String e080Sql = """
      SELECT appperid, appgubun
      FROM TB_E080
      WHERE appnum = :appnum
  """;
    List<Map<String, Object>> approvalLines = sqlRunner.getRows(e080Sql, params);

    // ✅ row 수가 2개 이상일 때만 체크
    if (approvalLines.size() > 1) {
      for (Map<String, Object> row : approvalLines) {
        Integer rowAppperid = (Integer) row.get("appperid");
        String rowAppgubun = (String) row.get("appgubun");

        if (!rowAppperid.equals(currentAppperid)) {
          if ("101".equals(rowAppgubun)) {
            log.warn("❌ 이미 다른 결재자가 승인(101)을 한 상태. 업데이트 불가.");
            return false;
          }
        }
      }
      log.info("✅ 다수 결재자 조건 만족: appnum={}, 요청자 appperid={}", appnum, currentAppperid);
    }

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
        indate = CONVERT(varchar(8), GETDATE(), 112)
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
  public boolean updateStateForNumberZZ(String appnum, String appgubun, String action, String remark) {
    // TODO: 실제 상태 변경 로직 구현 필요
    log.warn("🛠 [updateStateForNumberZZ] 임시 실행 - appnum: {}", appnum);
    return true; // 일단 성공으로 반환
  }


  // 휴가 문서 상태 변경 (TB_PB204, TB_E080)
  public boolean updateStateForV(String appnum, String appgubun, String action, String remark) {
    log.warn("🛠 [updateStateForV] 임시 실행 - appnum: {}", appnum);
    return true; // 추후 실제 업데이트 로직 구현 예정
  }

 /* public boolean updateStateForV(String appnum, String appgubun, String action, String remark) {
    log.info("🔍 휴가 문서 상태 변경 요청: {}", appnum);

    boolean existsInE080 = e080Repository.existsByAppnum(appnum);
    boolean existsInPB204 = tbPb204Repository.existsByAppnum(appnum);

    if (!existsInE080 || !existsInPB204) {
      log.warn("❗ appnum 존재 확인 실패: TB_E080={}, TB_PB204={}", existsInE080, existsInPB204);
      return false;
    }
    MapSqlParameterSource params = new MapSqlParameterSource();
    params.addValue("appnum", appnum);
    params.addValue("appgubun", appgubun);
    params.addValue("action", action);
    params.addValue("remark", remark);

    StringBuilder sql = new StringBuilder("""
       
        """);
    log.info("결재승인 SQL: {}", sql);
    log.info("SQL Parameters: {}", params.getValues());
    return sqlRunner.getRows(sql.toString(), params);
//    int updatedE080 = e080Repository.updateAppgubunByAppnum(appnum, appgubun, remark);
//    int updatedPB204 = tbPb204Repository.updateAppgubunByAppnum(appnum, appgubun, remark);

    boolean success = updatedE080 > 0 && updatedPB204 > 0;

    log.info("✅ 상태 변경 완료 - appnum={}, 성공여부={}", appnum, success);

    return success;
  }*/

  /*boolean existsByAppnum(String appnum);

@Modifying
@Transactional
@Query("UPDATE TB_E080 e SET e.appgubun = :appgubun, e.remark = :remark WHERE e.appnum = :appnum")
int updateAppgubunByAppnum(@Param("appnum") String appnum,
                           @Param("appgubun") String appgubun,
                           @Param("remark") String remark);

                           boolean existsByAppnum(String appnum);

@Modifying
@Transactional
@Query("UPDATE TB_PB204 p SET p.appgubun = :appgubun, p.appremark = :remark WHERE p.appnum = :appnum")
int updateAppgubunByAppnum(@Param("appnum") String appnum,
                           @Param("appgubun") String appgubun,
                           @Param("remark") String remark);

*/


}
