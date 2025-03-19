package mes.app.system.service;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Service;

import io.micrometer.core.instrument.util.StringUtils;
import mes.domain.services.SqlRunner;

@Service
public class MenuLogService {

	@Autowired
	SqlRunner sqlRunner;

	public List<Map<String, Object>> getLogCount(String dateFrom, String dateTo, String menuCode, String userPk) {

		MapSqlParameterSource paramMap = new MapSqlParameterSource();
		paramMap.addValue("dateFrom", dateFrom + " 00:00:00");
		paramMap.addValue("dateTo", dateTo + " 23:59:59");
		paramMap.addValue("menuCode", menuCode);
		paramMap.addValue("userPk", userPk);

		String sql = "SELECT mf.[FolderName] AS folder_name" +
				", g.[MenuCode] AS menu_code, m.[MenuName] AS menu_name" +
				", COUNT(*) AS use_count" +
				" FROM menu_use_log g" +
				" INNER JOIN menu_item m ON m.[MenuCode] = g.[MenuCode]" +
				" LEFT JOIN menu_folder mf ON mf.id = m.[MenuFolder_id]" +
				" LEFT JOIN auth_user u ON u.id = g.[User_id]" +
				" WHERE g._created BETWEEN CAST(:dateFrom AS DATETIME) AND CAST(:dateTo AS DATETIME)";

		if (StringUtils.isEmpty(menuCode) == false) {
			sql += " AND g.[MenuCode] = :menuCode";
		}
		if (StringUtils.isEmpty(userPk) == false) {
			sql += " AND g.[User_id] = CAST(:userPk AS INT)";
		}

		sql += " GROUP BY mf.[FolderName], g.[MenuCode], m.[MenuName]";

		List<Map<String,Object>> items = this.sqlRunner.getRows(sql, paramMap);

		return items;
	}

	public List<Map<String, Object>> getLogList(String dateFrom, String dateTo, String menuCode, String userPk) {
		MapSqlParameterSource paramMap = new MapSqlParameterSource();
		paramMap.addValue("dateFrom", dateFrom + " 00:00:00");
		paramMap.addValue("dateTo", dateTo + " 23:59:59");
		paramMap.addValue("menuCode", menuCode);
		paramMap.addValue("userPk", userPk);

		String sql = """
        SELECT
            ROW_NUMBER() OVER (ORDER BY g._created DESC) AS row_number,
            g.id,
            mf.[FolderName] AS folder_name,
            g.[MenuCode] AS menu_code,
            m.[MenuName] AS menu_name,
            u.username,
            p.[Name] AS user_name,
            FORMAT(g._created, 'yyyy-MM-dd HH:mm:ss') AS click_date
        FROM
            menu_use_log g
            INNER JOIN menu_item m ON m.[MenuCode] = g.[MenuCode]
            LEFT JOIN menu_folder mf ON mf.id = m.[MenuFolder_id]
            LEFT JOIN auth_user u ON u.id = g.[User_id]
            LEFT JOIN user_profile p ON p.[User_id] = g.[User_id]
        WHERE
            g._created BETWEEN CAST(:dateFrom AS datetime) AND CAST(:dateTo AS datetime)
        """;

		if (StringUtils.isNotEmpty(menuCode)) {
			sql += " AND g.[MenuCode] = :menuCode ";
		}
		if (StringUtils.isNotEmpty(userPk)) {
			sql += " AND g.[User_id] = CAST(:userPk AS INT) ";
		}
		sql += " ORDER BY g._created DESC ";

		List<Map<String, Object>> items = this.sqlRunner.getRows(sql, paramMap);

		return items;
	}

	public List<Map<String, Object>> getUserList() {

		String sql = """ 
       SELECT u.id AS value,
       u.username + '(' + u.last_name + u.first_name + ')' AS text
		FROM auth_user u
		INNER JOIN user_profile up ON up.[User_id] = u.id
		INNER JOIN user_group ug ON ug.id = up.[UserGroup_id]
		WHERE u.is_active = 1
		  AND u.is_superuser = 0
		  AND ug.[Code] NOT IN ('dev')
		ORDER BY 2;
		""";

		List<Map<String,Object>> items = this.sqlRunner.getRows(sql, null);

		return items;
	}

}
