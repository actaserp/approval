package mes.app.alarm.Service;

import lombok.extern.slf4j.Slf4j;
import mes.domain.entity.UserGroup;
import mes.domain.services.SqlRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class AlarmService {

    @Autowired
    SqlRunner sqlRunner;


    public List<Map<String, Object>> getNotifications(String userId, String spjangcd, int userGroupId) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("spjangcd", spjangcd);
        params.addValue("perid", userId);

        String flagCondition = "";
        if (userGroupId == 35) {
            flagCondition = "tb006.userflag = '0'";
        } else if (userGroupId == 1 || userGroupId == 14) {
            flagCondition = "tb006.adflag = '0'";
        }

        String sql = """
        SELECT
                    e.indate AS alert_date,
                    e.title,
                    e.appgubun
                FROM
                    TB_E080 e
                WHERE
                    e.adflag = '0'           -- spjangcd가 0인 데이터만 가져오기
                    AND e.spjangcd = :spjangcd
                    AND e.appperid = :perid
                ORDER BY
                    e.indate DESC;
        """;

        return sqlRunner.getRows(sql, params);
    }

    /*public void markAsRead(String userId, String spjangcd, UserGroup userGroupId) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("spjangcd", spjangcd);

        // 조건에 따라 업데이트할 필드 설정
        String flagColumn;
        if (userGroupId.getId() == 35) {
            flagColumn = "userflag";
        } else if (userGroupId.getId() == 1 || userGroupId.getId() == 14) {
            flagColumn = "adflag";
        } else {
            throw new IllegalArgumentException("유효하지 않은 UserGroup ID입니다.");
        }

        // SQL 업데이트
        String sql =
                "UPDATE TB_DA006W " +
                        "SET " + flagColumn + " = '1' " +
                        "WHERE spjangcd = :spjangcd " +
                        "AND " + flagColumn + " = '0'";

        sqlRunner.execute(sql, params);
    }*/

    public void markAsRead(String userId, String spjangcd, UserGroup userGroupId) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("spjangcd", spjangcd);

        String flagColumn;
        String resetFlagColumn;

        if (userGroupId.getId() == 35) {
            flagColumn = "userflag";
            resetFlagColumn = "adflag";
        } else if (userGroupId.getId() == 1 || userGroupId.getId() == 14) {
            flagColumn = "adflag";
            resetFlagColumn = "userflag";
        } else {
            throw new IllegalArgumentException("유효하지 않은 UserGroup ID입니다.");
        }

        // 1. 지정된 flagColumn을 1로 업데이트
        String updateFlagSql =
                "UPDATE TB_E080 " +
                        "SET " + flagColumn + " = '1' " +
                        "WHERE spjangcd = :spjangcd " +
                        "AND " + flagColumn + " = '0'";

        sqlRunner.execute(updateFlagSql, params);

        // 2. 다른 플래그를 0으로 설정
        String resetFlagSql =
                "UPDATE TB_DA006W " +
                        "SET " + resetFlagColumn + " = '0' " +
                        "WHERE spjangcd = :spjangcd";

        sqlRunner.execute(resetFlagSql, params);
    }
    // 사용자 사원코드 조회(맨앞 'p'제거 필요)
    public String getPerid(String username) {
        MapSqlParameterSource dicParam = new MapSqlParameterSource();

        String sql = """
                SELECT perid
                FROM tb_xusers
                WHERE userid = :username
                """;
        dicParam.addValue("username", username);
        Map<String, Object> perid = this.sqlRunner.getRow(sql, dicParam);
        String Perid = "";
        if(perid != null && perid.containsKey("perid")) {
            Perid = (String) perid.get("perid");
        }
        return Perid;
    }


}


    
