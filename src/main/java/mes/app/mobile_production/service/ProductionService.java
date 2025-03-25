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
                select xs.custcd,
                       xs.spjangcd
                FROM TB_XUSERS xs
                WHERE xs.userid = :username
                """;
        dicParam.addValue("username", username);
        Map<String, Object> userInfo = this.sqlRunner.getRow(sql, dicParam);
        return userInfo;
    }

    // 회계전표 데이터 불러오기
    public List<Map<String, Object>> getProductionList(Map<String, Object> searchLabels) {
        String searchSpjangcd = (String) searchLabels.get("search_spjangcd");
        String searchStartdate = (String) searchLabels.get("search_startdate");
        String searchEnddate = (String) searchLabels.get("search_enddate");
        String searchSubject = (String) searchLabels.get("search_subject");
        String searchGubun = (String) searchLabels.get("search_gubun");
        MapSqlParameterSource dicParam = new MapSqlParameterSource();
        List<Map<String, Object>> items = new ArrayList<>();

        dicParam.addValue("search_spjangcd", searchSpjangcd);
        dicParam.addValue("search_startdate", searchStartdate);
        dicParam.addValue("search_enddate", searchEnddate);

        StringBuilder sql = new StringBuilder("""
                SELECT A.custcd  ,  --회사코드
                		 A.spjangcd,  --사업장코드
                		 A.spdate  ,  --전표일자
                		 A.spnum   , --전표번호
                		 A.tiosec  ,  --세입세출구분
                		SUM(B.dramt)   AS dramt,  --  차변금액
                		SUM(B.cramt)   AS cramt,    -- 대변금액
                		 MIN(B.comnote) AS summy, --적요
                		 A.subject,     -- 제목
                		 A. mssec,     -- 재원
                		 A.appdate		, --  결재상신일자
                		 A.appperid		,  -- 결재상신 사원번호
                		 A.appgubun		,  -- 결재구분
                		 A.appnum		,   -- 결재번호
                		 (select mssecnm from tb_x0005 where mssec=min(B.mssec)) as mssecnm,
                		 C.appgubun ,
                		 C.appnum ,
                		 C.title
                  FROM TB_AA009 A WITH (NOLOCK) ,
                		 TB_AA010 B WITH (NOLOCK),
                		 TB_E080 C
                 WHERE (A.custcd   = B.custcd    )
                	AND (A.spjangcd = B.spjangcd  )
                	AND (A.spdate   = B.spdate    )
                	AND (A.spnum    = B.spnum     )
                	AND (A.spjangcd = :searchSpjangcd)
                	AND (A.spdate   BETWEEN :search_startdate AND :search_enddate)    --  검색 : 일자
                	AND (isnull(A.subject,'')   LIKE :searchSubject)  -- 검색 : 제목
                	AND C.appgubun = :searchGubun
                	AND A.spdate + A.spnum + A.spjangcd = C.appnum
                
                GROUP BY A.custcd  ,
                		    A.spjangcd,
                		    A.spdate  ,
                		 	 A.spnum   ,
                		 	 A.tiosec  ,
                		 	 A.mssec,
                		 A.subject,
                		 A.appdate		,
                		 A.appperid		,
                		 A.appgubun		,
                		 A.appnum		,
                		 C.appgubun ,
                		 C.appnum ,
                		 C.title
                """);

        if(!searchSubject.isEmpty()) {
            dicParam.addValue("searchSubject","%" + searchSubject + "%");
        }else {
            dicParam.addValue("searchSubject", "%");
        }
        if(!searchGubun.isEmpty()) {
            dicParam.addValue("searchGubun",searchGubun);
        }else {
            dicParam.addValue("searchGubun", "%");
        }


        try {
            items = this.sqlRunner.getRows(String.valueOf(sql), dicParam);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return items != null ? items : List.of();
    }

    // 지출결의서 데이터 조회
    public List<Map<String, Object>> getJichulList(Map<String, Object> searchLabels) {
        String searchSpjangcd = (String) searchLabels.get("search_spjangcd");
        String searchStartdate = (String) searchLabels.get("search_startdate");
        String searchEnddate = (String) searchLabels.get("search_enddate");
        String searchSubject = (String) searchLabels.get("search_subject");
        String searchGubun = (String) searchLabels.get("search_gubun");
        MapSqlParameterSource dicParam = new MapSqlParameterSource();
        List<Map<String, Object>> items = new ArrayList<>();

        dicParam.addValue("search_spjangcd", searchSpjangcd);
        dicParam.addValue("search_startdate", searchStartdate);
        dicParam.addValue("search_enddate", searchEnddate);

        StringBuilder sql = new StringBuilder("""
                SELECT A.custcd  ,  --회사코드
                		 A.spjangcd,  --사업장코드
                		 A.spdate  ,  --전표일자
                		 A.spnum   , --전표번호
                		 A.tiosec  ,  --세입세출구분
                		SUM(B.dramt)   AS dramt,  --  차변금액
                		SUM(B.cramt)   AS cramt,    -- 대변금액
                		 MIN(B.comnote) AS summy, --적요
                		 A.subject,     -- 제목
                		 A. mssec,     -- 재원
                		 A.appdate		, --  결재상신일자
                		 A.appperid		,  -- 결재상신 사원번호
                		 A.appgubun		,  -- 결재구분
                		 A.appnum		,   -- 결재번호
                		 (select mssecnm from tb_x0005 where mssec=min(B.mssec)) as mssecnm,
                		 C.appgubun ,
                		 C.appnum ,
                		 C.title
                  FROM TB_AA009 A WITH (NOLOCK) ,
                		 TB_AA010 B WITH (NOLOCK),
                		 TB_E080 C
                 WHERE (A.custcd   = B.custcd    )
                	AND (A.spjangcd = B.spjangcd  )
                	AND (A.spdate   = B.spdate    )
                	AND (A.spnum    = B.spnum     )
                	AND (A.spjangcd = :searchSpjangcd)
                	AND (A.spdate   BETWEEN :search_startdate AND :search_enddate)    --  검색 : 일자
                	AND (isnull(A.subject,'')   LIKE :searchSubject)  -- 검색 : 제목
                	AND C.appgubun = :searchGubun
                	AND A.spdate + A.spnum + A.spjangcd = substring(C.appnum,2,20)
                
                GROUP BY A.custcd  ,
                		    A.spjangcd,
                		    A.spdate  ,
                		 	 A.spnum   ,
                		 	 A.tiosec  ,
                		 	 A.mssec,
                		 A.subject,
                		 A.appdate		,
                		 A.appperid		,
                		 A.appgubun		,
                		 A.appnum		,
                		 C.appgubun ,
                		 C.appnum ,
                		 C.title
                """);

        if(!searchSubject.isEmpty()) {
            dicParam.addValue("searchSubject","%" + searchSubject + "%");
        }else {
            dicParam.addValue("searchSubject", "%");
        }
        if(!searchGubun.isEmpty()) {
            dicParam.addValue("searchGubun",searchGubun);
        }else {
            dicParam.addValue("searchGubun", "%");
        }


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
