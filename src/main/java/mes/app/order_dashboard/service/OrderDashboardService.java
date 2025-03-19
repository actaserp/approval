package mes.app.order_dashboard.service;

import mes.domain.entity.actasEntity.TB_DA006W_PK;
import mes.domain.services.SqlRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
public class OrderDashboardService {

    @Autowired
    SqlRunner sqlRunner;

    // username으로 cltcd, cltnm, saupnum, custcd 가지고 오기
    public Map<String, Object> getUserInfo(String username) {
        MapSqlParameterSource dicParam = new MapSqlParameterSource();

        String sql = """
                select xc.custcd,
                       xc.cltcd,
                       xc.cltnm,
                       xc.saupnum,
                       au.spjangcd
                FROM TB_XCLIENT xc
                left join auth_user au on au."username" = xc.saupnum
                WHERE xc.saupnum = :username
                """;
        dicParam.addValue("username", username);
        Map<String, Object> userInfo = this.sqlRunner.getRow(sql, dicParam);
        return userInfo;
    }

    //주문의뢰현황 불러오기
    public List<Map<String, Object>> getOrderList(TB_DA006W_PK tbDa006W_pk,
                                                  String searchStartDate, String searchEndDate, String searchType, String saupnum, String cltcd) {

        MapSqlParameterSource dicParam = new MapSqlParameterSource();
        dicParam.addValue("searchStartDate", searchStartDate);
        dicParam.addValue("searchEndDate", searchEndDate);
        dicParam.addValue("searchType", searchType);
        dicParam.addValue("custcd", tbDa006W_pk.getCustcd());
        dicParam.addValue("spjangcd", tbDa006W_pk.getSpjangcd());
        dicParam.addValue("saupnum", saupnum);
        dicParam.addValue("cltcd", cltcd);

        StringBuilder sql = new StringBuilder("""
                SELECT
                    custcd,
                    spjangcd,
                    reqnum,
                    reqdate,
                    ordflag,
                    deldate,
                    telno,
                    perid,
                    cltzipcd,
                    cltaddr,
                    remark
                FROM
                    TB_DA006W hd
                WHERE
                    hd.custcd = :custcd
                    AND hd.spjangcd = :spjangcd
                    AND hd.saupnum = :saupnum
                    AND hd.cltcd = :cltcd
                """);
        // 날짜 필터
        if (searchStartDate != null && !searchStartDate.isEmpty()) {
            sql.append(" AND reqdate >= :searchStartDate");
        }
        //
        if (searchEndDate != null && !searchEndDate.isEmpty()) {
            sql.append(" AND reqdate <= :searchEndDate");
        }
        // 진행구분 필터
        if (searchType != null && !searchType.isEmpty()) {
            sql.append(" AND ordflag LIKE :searchType");
        }
        // 정렬 조건 추가
        sql.append(" ORDER BY reqdate ASC");

        List<Map<String, Object>> items = this.sqlRunner.getRows(sql.toString(), dicParam);
        return items;
    }

    public List<Map<String, Object>> initDatas(String as_perid) {
        MapSqlParameterSource dicParam = new MapSqlParameterSource();
        StringBuilder sql = new StringBuilder("""
                SELECT
                    (SELECT COUNT(appgubun)
                     FROM S_KRU.dbo.TB_E080 WITH(NOLOCK)
                     WHERE appgubun = '001'
                     AND appperid = :as_perid
                     AND flag = '1'
                     AND repodate BETWEEN :as_stdate AND :as_enddate) AS appgubun1, -- 대기
                
                    (SELECT COUNT(appgubun)
                     FROM S_KRU.dbo.TB_E080 WITH(NOLOCK)
                     WHERE appgubun = '101'
                     AND appperid = :as_perid
                     AND flag = '1'
                     AND repodate BETWEEN :as_stdate AND :as_enddate) AS appgubun2, -- 결제
                
                    (SELECT COUNT(appgubun)
                     FROM S_KRU.dbo.TB_E080 WITH(NOLOCK)
                     WHERE appgubun = '131'
                     AND appperid = :as_perid
                     AND flag = '1'
                     AND repodate BETWEEN :as_stdate AND :as_enddate) AS appgubun3, -- 반려
                
                    (SELECT COUNT(appgubun)
                     FROM S_KRU.dbo.TB_E080 WITH(NOLOCK)
                     WHERE appgubun = '201'
                     AND appperid = :as_perid
                     AND flag = '1'
                     AND repodate BETWEEN :as_stdate AND :as_enddate) AS appgubun4 -- 보류
                """);
        // 현재 연도의 1월 1일과 12월 31일을 자동으로 설정
        LocalDate today = LocalDate.now();
        String startDate = today.withDayOfYear(1).format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String endDate = today.withMonth(12).withDayOfMonth(31).format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        dicParam.addValue("as_perid", as_perid);
        dicParam.addValue("as_stdate", startDate);
        dicParam.addValue("as_enddate", endDate);

        List<Map<String, Object>> items = this.sqlRunner.getRows(sql.toString(), dicParam);
        return items;
    }
}
