package mes.app.mobile_production.service;

import mes.domain.entity.actasEntity.TB_DA006W_PK;
import mes.domain.model.AjaxResult;
import mes.domain.services.SqlRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Service;

import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ProductionService {
    @Autowired
    SqlRunner sqlRunner;

    // username으로 cltcd, cltnm, saupnum, custcd 가지고 오기
    public Map<String, Object> getUserInfo(String username) {
        MapSqlParameterSource dicParam = new MapSqlParameterSource();

        String sql = """
                SELECT *
                FROM tb_xusers
                WHERE userid = :username
                """;
        dicParam.addValue("username", username);
        Map<String, Object> userInfo = this.sqlRunner.getRow(sql, dicParam);
        return userInfo;
    }

    // 회계전표 + 지출결의서 데이터 불러오기
    public List<Map<String, Object>> getProductionList(Map<String, Object> searchLabels) {
        String searchSpjangcd = (String) searchLabels.get("search_spjangcd");
        String searchStartdate = (String) searchLabels.get("search_startDate");
        String searchEnddate = (String) searchLabels.get("search_endDate");
        String searchSubject = (String) searchLabels.get("search_subject");
        String searchGubun = (String) searchLabels.get("search_gubun");
        String searchPerid = (String) searchLabels.get("search_perid");
        MapSqlParameterSource dicParam = new MapSqlParameterSource();
        List<Map<String, Object>> items = new ArrayList<>();

        dicParam.addValue("search_spjangcd", searchSpjangcd);
        dicParam.addValue("search_startdate", searchStartdate);
        dicParam.addValue("search_enddate", searchEnddate);
        dicParam.addValue("search_perid", searchPerid);

        dicParam.addValue("searchSubject",
                (searchSubject != null && !searchSubject.isEmpty()) ? "%" + searchSubject + "%" : "%");

        dicParam.addValue("searchGubun",
                (searchGubun != null && !searchGubun.isEmpty()) ? "%" + searchGubun + "%" : "%");


        StringBuilder sql = new StringBuilder("""
                    SELECT *,
                            '1' as flag
                          FROM (
                              -- 첫 번째 쿼리
                              SELECT
                                  A.custcd,
                                  A.spjangcd,
                                  A.spdate,
                                  A.spnum,
                                  A.tiosec,
                                  SUM(B.dramt) AS dramt,
                                  SUM(B.cramt) AS cramt,
                                  MIN(B.comnote) AS summy,
                                  A.subject,
                                  A.mssec,
                                  X.mssecnm,
                                  A.appdate,
                                  A.appperid,
                                  A.appgubun,
                                  C.appnum AS e080_appnum,
                                  C.appgubun AS e080_appgubun,
                                  C.title AS e080_title
                              FROM
                                  TB_AA007 A WITH (NOLOCK)
                              JOIN
                                  TB_AA008 B WITH (NOLOCK)
                                  ON A.custcd = B.custcd
                                  AND A.spjangcd = B.spjangcd
                                  AND A.spdate = B.spdate
                                  AND A.spnum = B.spnum
                              LEFT JOIN
                                  TB_E080 C
                                  ON C.appnum =  'S' + A.spdate + A.spnum + A.spjangcd
                                  AND C.spjangcd = A.spjangcd
                                  AND C.custcd = A.custcd
                              OUTER APPLY (
                                  SELECT TOP 1 mssecnm
                                  FROM tb_x0005
                                  WHERE mssec = B.mssec
                                  ORDER BY mssec
                              ) X
                              WHERE
                                  A.spjangcd = :search_spjangcd
                                  AND A.spdate BETWEEN :search_startdate AND :search_enddate
                                  AND (:searchSubject = '%' OR A.subject LIKE :searchSubject)
                                  AND (:searchGubun = '%' OR C.appgubun LIKE :searchGubun)
                                  AND C.appperid = :search_perid
                              GROUP BY
                                  A.custcd, A.spjangcd, A.spdate, A.spnum, A.tiosec,
                                  A.mssec, A.subject, A.appdate, A.appperid, A.appgubun,
                                  C.appnum, X.mssecnm, C.appgubun, C.title
                
                              UNION ALL
                
                              -- 두 번째 쿼리
                              SELECT
                                  A.custcd,
                                  A.spjangcd,
                                  A.spdate,
                                  A.spnum,
                                  A.tiosec,
                                  SUM(B.dramt) AS dramt,
                                  SUM(B.cramt) AS cramt,
                                  MIN(B.comnote) AS summy,
                                  A.subject,
                                  A.mssec,
                                  X.mssecnm,
                                  A.appdate,
                                  A.appperid,
                                  A.appgubun,
                                  C.appnum,
                                  C.appgubun AS e080_appgubun,
                                  C.title AS e080_title
                              FROM
                                  TB_AA009 A WITH (NOLOCK)
                              JOIN
                                  TB_AA010 B WITH (NOLOCK)
                                  ON A.custcd = B.custcd
                                  AND A.spjangcd = B.spjangcd
                                  AND A.spdate = B.spdate
                                  AND A.spnum = B.spnum
                              LEFT JOIN
                                  TB_E080 C
                                  ON C.appnum = A.spdate + A.spnum + A.spjangcd
                                  AND C.spjangcd = A.spjangcd
                                  AND C.custcd = A.custcd
                              OUTER APPLY (
                                  SELECT TOP 1 mssecnm
                                  FROM tb_x0005
                                  WHERE mssec = B.mssec
                                  ORDER BY mssec
                              ) X
                              WHERE
                                  A.spjangcd = :search_spjangcd
                                  AND A.spdate BETWEEN :search_startdate AND :search_enddate
                                  AND (:searchSubject = '%' OR A.subject LIKE :searchSubject)
                                  AND (:searchGubun = '%' OR C.appgubun LIKE :searchGubun)
                                  AND C.appperid = :search_perid
                              GROUP BY
                                  A.custcd, A.spjangcd, A.spdate, A.spnum, A.tiosec,
                                  A.mssec, A.subject, A.appdate, A.appperid, A.appgubun,
                                  C.appnum, X.mssecnm, C.appgubun, C.title
                          ) AS UNION_RESULT
                          ORDER BY spdate DESC, spnum DESC
                """);

        try {
            items = this.sqlRunner.getRows(String.valueOf(sql), dicParam);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return items != null ? items : List.of();
    }

    // 휴가신청서 데이터 조회
    public List<Map<String, Object>> getVacList(Map<String, Object> searchLabels) {
        String searchSpjangcd = (String) searchLabels.get("search_spjangcd");
        String searchStartdate = (String) searchLabels.get("search_startDate");
        String searchEnddate = (String) searchLabels.get("search_endDate");
        String searchSubject = (String) searchLabels.get("search_subject");
        String searchGubun = (String) searchLabels.get("search_gubun");
        String searchPerid = (String) searchLabels.get("search_perid");
        MapSqlParameterSource dicParam = new MapSqlParameterSource();
        List<Map<String, Object>> items = new ArrayList<>();

        dicParam.addValue("search_spjangcd", searchSpjangcd);
        dicParam.addValue("search_startdate", searchStartdate);
        dicParam.addValue("search_enddate", searchEnddate);
        dicParam.addValue("search_perid", searchPerid);

        dicParam.addValue("searchSubject",
                (searchSubject != null && !searchSubject.isEmpty()) ? "%" + searchSubject + "%" : "%");

        dicParam.addValue("searchGubun",
                (searchGubun != null && !searchGubun.isEmpty()) ? "%" + searchGubun + "%" : "%");

        StringBuilder sql = new StringBuilder("""
                SELECT
                 '2' as flag,
                  A.reqdate,
                  A.custcd,
                  A.spjangcd,
                  A.vayear,
                  A.vanum,
                  A.vafrdate,
                  A.vatodate,
                  A.vafrtime,
                  A.vatotime,
                  A.perid,
                  A.reqdate,
                  A.reasontxt,
                  A.remark,
                  A.gowhere,
                  A.appdate,
                  A.appgubun,
                  A.appperid,
                  A.appremark,
                  A.appnum,
                  A.yearflag,
                  A.reqdate AS spdate,
                  C.appgubun AS e080_appgubun,
                  C.appnum   AS e080_appnum,
                  C.title    AS e080_title,
                  X.pernm AS apppernm
                FROM TB_PB204 A WITH (NOLOCK)
                LEFT JOIN TB_E080 C
                  ON 'V' + A.vayear + A.vanum + A.spjangcd = C.appnum
                  AND A.spjangcd = C.spjangcd
                  AND A.custcd = C.custcd
                LEFT JOIN tb_xusers X
                    ON 'p' + A.perid = X.perid
                WHERE A.spjangcd = :search_spjangcd
                AND A.reqdate BETWEEN :search_startdate AND :search_enddate
                AND (:searchSubject = '%' OR A.remark LIKE :searchSubject)
                AND (:searchGubun = '%' OR C.appgubun LIKE :searchGubun)
                AND C.appperid = :search_perid
                ORDER BY A.reqdate DESC, A.vanum DESC;
                """);

        try {
            items = this.sqlRunner.getRows(String.valueOf(sql), dicParam);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return items != null ? items : List.of();
    }
    //작지리스트 불러오기
    public List<Map<String, Object>> getWorkList(Map<String, Object> searchLabels) {

        MapSqlParameterSource dicParam = new MapSqlParameterSource();
        List<Map<String, Object>> items = new ArrayList<>();
        String sql = "  SELECT CONVERT(CHAR(8), TB_FPLAN_WORK.wtrdt, 112) AS wtrdt,   \n" +
                "       TB_FPLAN.plan_no,   \n" +
                "       TB_FPLAN.wono,   \n" +
                "       TB_FPLAN.pcode,   \n" +
                "       TB_CA501.phm_pnam AS pname,   \n" +
                "       TB_CA501.phm_size AS psize,   \n" +
                "       TB_CA501.phm_unit AS punit,   \n" +
                "       TB_FPLAN.cltcd,\n" +
                "       TB_FPLAN_WORK.wflag AS wflag,\n" +
                "       TB_FPLAN_WORK.wotqt AS wotqt,   \n" +
                "       TB_FPLAN_WORK.wbdqt AS wbdqt,   \n" +
                "       TB_FPLAN_WORK.wotqt - TB_FPLAN_WORK.wbdqt AS wokqt,   \n" +
                "       TB_FPLAN_WORK.wremark AS remark\n" +
                "FROM TB_FPLAN WITH(NOLOCK)\n" +
                "INNER JOIN TB_FPLAN_WORK WITH(NOLOCK) ON \n" +
                "       TB_FPLAN.custcd = TB_FPLAN_WORK.custcd AND\n" +
                "       TB_FPLAN.spjangcd = TB_FPLAN_WORK.spjangcd AND\n" +
                "       TB_FPLAN.plan_no = TB_FPLAN_WORK.plan_no\n" +
                "INNER JOIN TB_CA501 WITH(NOLOCK) ON \n" +
                "       TB_FPLAN.custcd = TB_CA501.phm_cust AND\n" +
                "       TB_FPLAN.pcode = TB_CA501.phm_pcod\n" +
                "WHERE TB_FPLAN.custcd = :custcd\n" +
                "  AND TB_FPLAN.spjangcd = :spjangcd\n" +
                "  AND TB_FPLAN.wono = :wono\n" +
                "  AND TB_FPLAN_WORK.decision = '0';";

        dicParam.addValue("custcd", searchLabels.get("search_custcd"));
        dicParam.addValue("spjangcd", searchLabels.get("search_spjangcd"));
        dicParam.addValue("wono", searchLabels.get("wono"));

        try {
            items = this.sqlRunner.getRows(sql, dicParam);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return items != null ? items : List.of();
    }

    //거래처 검색
    public List<Map<String, Object>> searchCltcd (String searchCltnm) {
        MapSqlParameterSource dicParam = new MapSqlParameterSource();
        List<Map<String, Object>> items = new ArrayList<>();
        String sql = "SELECT cltcd,   " + // 거래처 코드
                "       cltnm     " +   // 거래처명
                "  FROM TB_XCLIENT WITH(NOLOCK)" +
                " WHERE (cltcd  LIKE :searchCltcd" +
                "    OR  cltnm  LIKE :searchCltcd)";

        if(!searchCltnm.isEmpty()) {
            dicParam.addValue("searchCltcd", "%" + searchCltnm + "%");  // searchLabels.get("param1")
        }else {
            dicParam.addValue("searchCltcd", "%");
        }
        try {
            items = this.sqlRunner.getRows(sql, dicParam);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return items != null ? items : List.of();
    }

    // 품목 검색
    public List<Map<String, Object>> searchProduct(String searchProduct) {
        MapSqlParameterSource dicParam = new MapSqlParameterSource();
        List<Map<String, Object>> items = new ArrayList<>();
        String sql = "SELECT   TB_CA501.phm_pcod," + // 제품코드
                "       TB_CA501.phm_pnam, " + // 품명
                "       TB_CA501.phm_size   FROM TB_CA501 WITH(NOLOCK)" + // 규격
                " WHERE (TB_CA501.phm_pcod LIKE :searchProduct " +
                "    OR  TB_CA501.phm_pnam LIKE :searchProduct " +
                "    OR  TB_CA501.phm_size LIKE :searchProduct ) " +
                "   AND (TB_CA501.useyn    = '1')";

        if(!searchProduct.isEmpty()) {
            dicParam.addValue("searchProduct","%" + searchProduct + "%");
        }else {
            dicParam.addValue("searchProduct", "%");
        }


        try {
            items = this.sqlRunner.getRows(sql, dicParam);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return items != null ? items : List.of();
    }

    // wflag 매핑
    public Map<String, Object> getProcess(String comCode) {
        Map<String, Object> item = new HashMap<>();
        MapSqlParameterSource dicParam = new MapSqlParameterSource();
        String sql = "select com_cnam from TB_CA510 where com_cls = '040' and com_code<>'00' AND com_code = :com_code";

        dicParam.addValue("com_code", comCode);
        try {
            item = this.sqlRunner.getRow(sql, dicParam);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return item;
    }

    public List<Map<String, Object>> searchTodayGrid(Map<String, Object> searchLabels){
        List<Map<String, Object>> items = new ArrayList<>();
        MapSqlParameterSource dicParam = new MapSqlParameterSource();
        StringBuilder sql = new StringBuilder("""
                       SELECT
                           TB_FPLAN.cltcd,
                           DBO.DF_NM_RTN('Tb_XCLIENT', TB_FPLAN.custcd, TB_FPLAN.cltcd, '', '') cltnm,
                           TB_FPLAN.pcode,
                           DBO.DF_NM_RTN('Tb_CA501', TB_FPLAN.pcode, '', '', '') pname,
                           TB_FPLAN.prod_qty,
                           TB_FPLAN.end_qty,
                           CASE TB_FPLAN.cls_flag WHEN '1' THEN '미진행' WHEN '2' THEN '진행' WHEN '3' THEN '진행' WHEN '4' THEN '완료' END cls_flag
                       FROM {oj TB_FPLAN WITH(NOLOCK) LEFT OUTER JOIN TB_FPLAN_WORK WITH(NOLOCK)
                            ON TB_FPLAN.custcd = TB_FPLAN_WORK.custcd
                            AND TB_FPLAN.spjangcd = TB_FPLAN_WORK.spjangcd
                            AND TB_FPLAN.plan_no = TB_FPLAN_WORK.plan_no}
                       WHERE (TB_FPLAN.custcd     = :custcd)
                        AND (TB_FPLAN.spjangcd   = :spjangcd)
                        AND (TB_FPLAN.cls_flag   NOT IN ('0', '9'))
                        AND (TB_FPLAN.cltcd LIKE :cltcd)
                        AND (TB_FPLAN.pcode LIKE :pcode)
                        AND (TB_FPLAN_WORK.wstdt = :searchStartDate
                         OR  TB_FPLAN_WORK.wendt = :searchStartDate
                         OR (TB_FPLAN.prod_sdate = :searchStartDate AND TB_FPLAN.cls_flag = '1'))
                        ORDER BY 1, 3
                  """);

        dicParam.addValue("searchStartDate", searchLabels.get("search_startDate"));
        dicParam.addValue("custcd", searchLabels.get("search_custcd"));
        dicParam.addValue("spjangcd", searchLabels.get("search_spjangcd"));
        if(!searchLabels.get("search_cltcd").toString().isEmpty()) {
            dicParam.addValue("cltcd", searchLabels.get("search_cltcd"));
        }else {
            dicParam.addValue("cltcd", "%");
        }
        if(!searchLabels.get("search_pcode").toString().isEmpty()) {
            dicParam.addValue("pcode", searchLabels.get("search_pcode"));
        }else {
            dicParam.addValue("pcode", "%");
        }

        try {
            items = this.sqlRunner.getRows(sql.toString(), dicParam);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return items;
    }

}
