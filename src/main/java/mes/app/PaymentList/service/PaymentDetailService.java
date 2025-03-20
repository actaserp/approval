package mes.app.PaymentList.service;

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
                  e080.papercd,
                  e080.appgubun,
                  uc.Value AS appgubun_display,
                  e080.appdate,
                  e080.appnum,
                  e080.appperid,
                  e080.title,
                  e080.remark
                  FROM tb_e080 e080 WITH(NOLOCK)
                  left join user_code uc on uc.Code = e080.appgubun
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

    log.info("결재 목록 List SQL: {}", sql);
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
        SELECT (select count(appgubun) from tb_e080 WITH(NOLOCK) where appgubun = '001' AND appperid = :as_perid AND flag = '1' AND repodate Between :as_stdate AND :as_enddate) as appgubun1,
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
}
