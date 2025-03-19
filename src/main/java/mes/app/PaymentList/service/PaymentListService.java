package mes.app.PaymentList.service;

import lombok.extern.slf4j.Slf4j;
import mes.domain.services.SqlRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class PaymentListService {//결재목록

  @Autowired
  SqlRunner sqlRunner;

  public List<Map<String, Object>> getPaymentList(String spjangcd , String startDate, String endDate, String searchPayment, String searchUserNm) {
    MapSqlParameterSource params = new MapSqlParameterSource();
    params.addValue("as_spjangcd", spjangcd);
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

//    log.info("결재 목록 List SQL: {}", sql);
//    log.info("SQL Parameters: {}", params.getValues());
    return sqlRunner.getRows(sql.toString(), params);
  }


  // 사용자의 사업장코드 return
  public String getSpjangcd(String username
      , String searchSpjangcd) {
    MapSqlParameterSource dicParam = new MapSqlParameterSource();

    String sql = """
                SELECT spjangcd
                FROM auth_user
                WHERE username = :username
                """;
    dicParam.addValue("username", username);
    Map<String, Object> spjangcdMap = this.sqlRunner.getRow(sql, dicParam);
    String userSpjangcd = (String)spjangcdMap.get("spjangcd");

    String spjangcd = searchSpjangcd(searchSpjangcd, userSpjangcd);
    return spjangcd;
  }

  // init에 필요한 사업장코드 반환
  public String searchSpjangcd(String searchSpjangcd, String userSpjangcd){

    String resultSpjangcd = "";
    switch (searchSpjangcd){
      case "ZZ":
        resultSpjangcd = searchSpjangcd;
        break;
      case "PP":
        resultSpjangcd= searchSpjangcd;
        break;
      default:
        resultSpjangcd = userSpjangcd;
    }
    return resultSpjangcd;
  }

}
