package mes.app.system.service;


import groovy.transform.AutoFinal;
import mes.domain.services.SqlRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.Map;

import java.util.List;

@Service
public class AuthListService {


    @Autowired
    SqlRunner sqlRunner;


    public List<Map<String, Object>> getAuthList(String searchusr, Timestamp searchfrdate, Timestamp searchtodate, String searchflag, String searchuserid){
        MapSqlParameterSource dicParam = new MapSqlParameterSource();

        String sql;

        if(searchflag == null){
            searchflag = "";
        }

        dicParam.addValue("paramusr", "%" +searchusr+ "%");
        dicParam.addValue("searchfrdate", searchfrdate);
        dicParam.addValue("searchtodate", searchtodate);
        dicParam.addValue("searchflag", "%" +searchflag+ "%");
        dicParam.addValue("searchuserid", "%" +searchuserid+ "%");


        sql = """
                select *
                from TB_RP940  
                where 1=1
                AND "usernm" LIKE :paramusr
                AND "askdatem" BETWEEN :searchfrdate AND :searchtodate
                AND "appflag" LIKE :searchflag
                AND "userid" LIKE :searchuserid
                order by askdatem desc;
                """;

        List<Map<String, Object>> items = this.sqlRunner.getRows(sql, dicParam);
        return items;
    }



    public List<Map<String, Object>> getAuthspList(String userid){
        MapSqlParameterSource dicParam = new MapSqlParameterSource();

        String sql;


        dicParam.addValue("paramusr", userid);



        sql = """
                select *
                from TB_RP945  
                where 1=1
                AND "userid" = :paramusr
                order by askseq;
                """;

        List<Map<String, Object>> items = this.sqlRunner.getRows(sql, dicParam);
        return items;
    }

}
