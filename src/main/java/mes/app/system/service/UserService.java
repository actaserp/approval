package mes.app.system.service;

import io.micrometer.core.instrument.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import mes.app.account.service.TB_xusersService;
import mes.domain.DTO.UserDto;
import mes.domain.entity.User;
import mes.domain.entity.UserGroup;
import mes.domain.entity.actasEntity.TB_XUSERS;
import mes.domain.entity.actasEntity.TB_XUSERSId;
import mes.domain.model.AjaxResult;
import mes.domain.repository.UserGroupRepository;
import mes.domain.repository.UserRepository;
import mes.domain.repository.actasRepository.TB_XClientRepository;
import mes.domain.security.Pbkdf2Sha256;
import mes.domain.services.SqlRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
public class UserService {

    @Autowired
    SqlRunner sqlRunner;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private UserGroupRepository userGroupRepository;
    @Autowired
    TB_XClientRepository tbXClientRepository;
    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    TB_xusersService tbxusersService;

    // 사용자 리스트
    /*필요시
     *  WHERE
     * au.is_superuser = 0
     * 추가 하기 */
    public List<Map<String, Object>> getUserList(boolean superUser, String userGroup, String keyword) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        String sql = """
             SELECT
                  au.id,
                  au.first_name ,
                  au.username AS userid,
                  ug.id AS group_id,
                  au.spjangcd,
                  tx2.spjangnm,
                  tx.perid,
                  ug.Name AS group_name,
                  ug.id,
                  up.lang_code,
                  au.is_active,
                  au.Phone,
                  tx.pernm,
                   (select divinm from tb_jc002 where divicd=b.divicd and spjangcd=b.spjangcd) as divinm, 
                   (select rspnm from tb_pz001 where rspcd=b.rspcd and spjangcd=b.spjangcd) as rspnm, 
              FORMAT(au.date_joined, 'yyyy-MM-dd') AS date_joined
             FROM
                 auth_user au
             LEFT JOIN
                 user_profile up ON up.User_id = au.id
             LEFT JOIN
                 user_group ug ON ug.id = up.UserGroup_id
             LEFT JOIN
                 tb_xusers tx 
                 ON au.first_name = tx.pernm 
                 AND au.username = tx.userid
             LEFT join tb_xa012 tx2 on au.spjangcd = tx2.spjangcd
             LEFT JOIN tb_ja001 b on au.agencycd = b.perid and b.spjangcd=au.spjangcd
             WHERE 1 = 1
                """;

        if (!StringUtils.isEmpty(userGroup)) {
            sql += " AND ug.[id] = :userGroup ";
            params.addValue("userGroup", userGroup);
        }

        if (!StringUtils.isEmpty(keyword)) {
            sql += " AND up.[Name] LIKE '%' + :name + '%' ";
            params.addValue("name", keyword);
        }

        // SQL 실행 후 결과 반환
//        log.info("사용자 관리 List SQL: {}", sql);
//        log.info("SQL Parameters: {}", params.getValues());
        return sqlRunner.getRows(sql, params);  // ✅ FIXED
    }


    // 사용자 상세정보 조회
    public Map<String, Object> getUserDetail(Integer id) {

        MapSqlParameterSource dicParam = new MapSqlParameterSource();
        dicParam.addValue("id", id);

        String sql = """
                select au.id
                           , up."Name"
                           , au.username as login_id
                           , au.email
                           , ug."Name" as group_name
                           , up."UserGroup_id"
                           , up."Factory_id"
                           , f."Name" as factory_name
                           , d."Name" as dept_name
                           , up."Depart_id"
                           , up.lang_code
                           , au.is_active
                           , to_char(au.date_joined ,'yyyy-mm-dd hh24:mi') as date_joined
                         from auth_user au 
                         left join user_profile up on up."User_id" = au.id
                         left join user_group ug on up."UserGroup_id" = ug.id 
                         left join factory f on up."Factory_id" = f.id 
                         left join depart d on d.id = up."Depart_id"
                         where au.id = :id
                """;

        Map<String, Object> item = this.sqlRunner.getRow(sql, dicParam);

        return item;
    }

    // 사용자 그룹 조회
    public List<Map<String, Object>> getUserGrpList(Integer id) {

        MapSqlParameterSource dicParam = new MapSqlParameterSource();
        dicParam.addValue("id", id);

        String sql = """
                select ug.id as grp_id
                   , ug."Name" as grp_name
                   ,rd."Char1" as grp_check
                   from user_group ug 
                   left join rela_data rd on rd."DataPk2" = ug.id 
                   and "RelationName" = 'auth_user-user_group' 
                   and rd."DataPk1" = :id
                   where coalesce(ug."Code",'') <> 'dev'
                """;

        List<Map<String, Object>> items = this.sqlRunner.getRows(sql, dicParam);
        return items;
    }

    public Boolean SaveUser(User user, Authentication auth, String authType, String authCode) {

        try {
            User UserEntity = userRepository.save(user);

            List<UserGroup> AuthGroup = userGroupRepository.findByCodeAndName(authType, authCode);


            MapSqlParameterSource dicParam = new MapSqlParameterSource();


            User loginUser = (User) auth.getPrincipal();


            dicParam.addValue("loginUser", loginUser.getId());

            String sql = """
                    	INSERT INTO user_profile 
                    	("_created", "_creater_id", "User_id", "lang_code", "Name", "UserGroup_id" ) 
                    	VALUES (now(), :loginUser, :User_id, :lang_code, :name, :UserGroup_id )
                    """;

            dicParam.addValue("name", user.getFirst_name());
            dicParam.addValue("lang_code", "ko-KR");
            //dicParam.addValue("UserGroup_id", );
            dicParam.addValue("User_id", UserEntity.getId());
            dicParam.addValue("lang_code", "ko-KR");
            dicParam.addValue("UserGroup_id", AuthGroup.get(0).getId());

            this.sqlRunner.execute(sql, dicParam);
            return true;
        } catch (Exception e) {
            e.getMessage();
            return false;
        }


    }

    public List<Map<String, Object>> getUserSandanList(String id) {

        MapSqlParameterSource dicParam = new MapSqlParameterSource();
        dicParam.addValue("id", id);

        String sql = """
                SELECT t.userid,
                       t.spcompid AS spcompcd,
                       u_c."Value" AS spcompnm,
                       t.spplanid AS spplancd,
                       u_p."Value" AS spplannm,
                       t.spworkid AS spworkcd,
                       u_w."Value" AS spworknm,
                       t.askseq
                FROM tb_rp945 AS t
                LEFT JOIN user_code u_c ON t.spcompid = u_c.id
                LEFT JOIN user_code u_p ON t.spplanid = u_p.id
                LEFT JOIN user_code u_w ON t.spworkid = u_w.id
                WHERE t.userid = :id
                ORDER BY t.askseq;
                """;

        List<Map<String, Object>> items = this.sqlRunner.getRows(sql, dicParam);
        return items;
    }

    @Transactional
    public void save(User user) {
        userRepository.save(user);
    }

    public Map<String, Object> getUserDetailById(String id) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("id", id);

        String sql = """
                select
                    *
                from
                    TB_XCLIENT tx
                LEFT JOIN 
                    auth_user au on tx.prenm = au.first_name
                where 
                    au.id = :id;                
                """;
        return sqlRunner.getRow(sql, params);
    }

    public boolean isUserIdExists(String userid) {
        return userRepository.existsByUsername(userid); // username 컬럼 확인
    }

    // 내정보리스트
    public Map<String, Object> getUserInform(String username) {
        MapSqlParameterSource paramMap = new MapSqlParameterSource();
        paramMap.addValue("username", username);

        String sql = """
       SELECT
           au.username AS userid,
           xu.pernm AS usernm,
           xu.perid,
           (select divinm from tb_jc002 where divicd=b.divicd and spjangcd=b.spjangcd) as divinm,\s
         (select rspnm from tb_pz001 where rspcd=b.rspcd and spjangcd=b.spjangcd) as rspnm,\s
           xu.custcd
           FROM auth_user au
           left join TB_XUSERS xu on au.username = xu.userid
           LEFT JOIN tb_ja001 b on au.agencycd = b.perid and b.spjangcd=au.spjangcd
       WHERE username = :username;
    """;

        List<Map<String, Object>> rows = sqlRunner.getRows(sql, paramMap);
        return rows.isEmpty() ? null : rows.get(0);
    }

    //주소
    public Map<String, String> splitAddress(String fullAddress) {
        Map<String, String> addressParts = new HashMap<>();
        if (fullAddress != null && !fullAddress.isEmpty()) {
            int delimiterIndex = fullAddress.indexOf(" | "); // ' | ' 위치 찾기
            if (delimiterIndex != -1) {
                // ' | '를 기준으로 주소 분리
                addressParts.put("address1", fullAddress.substring(0, delimiterIndex).trim());
                addressParts.put("address2", fullAddress.substring(delimiterIndex + 3).trim());
            } else {
                // ' | '가 없으면 전체를 address1로 간주
                addressParts.put("address1", fullAddress.trim());
                addressParts.put("address2", "");
            }
        } else {
            // fullAddress가 비어있을 경우 기본값 설정
            addressParts.put("address1", "");
            addressParts.put("address2", "");
        }
        return addressParts;
    }

    public List<Map<String, Object>> getCustcdAndSpjangcd(String spjangcd) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("spjangcd", spjangcd);

        String sql = """
                select custcd, spjangcd 
                from tb_xa012 
                where spjangcd = :spjangcd
                """;

        return sqlRunner.getRows(sql, params);
    }

    // 사용자 정보 수정
    @Transactional
    public AjaxResult updateUserInfo(UserDto userDto) {
        AjaxResult result = new AjaxResult();

        // 1. User 테이블 업데이트
        Optional<User> user = userRepository.findByUsername(userDto.getUserid());
        if (user.isPresent()) {
            User userEntity = user.get();

            // 기존 값 유지 처리
            userEntity.setEmail(userDto.getEmail() != null ? userDto.getEmail() : userEntity.getEmail());
            userEntity.setFirst_name(userDto.getFirst_name() != null ? userDto.getFirst_name() : userEntity.getFirst_name());
            userEntity.setLast_name(userDto.getLast_name() != null ? userDto.getLast_name() : userEntity.getLast_name());
            userEntity.setPhone(userDto.getPhone() != null ? userDto.getPhone() : userEntity.getPhone());

            // 비밀번호 처리
            if (userDto.getPw() != null && !userDto.getPw().isEmpty()) {
                String encodedPw = Pbkdf2Sha256.encode(userDto.getPw());
                userEntity.setPassword(encodedPw);
            }

            // 업데이트
            userRepository.save(userEntity);
            log.info("User 테이블 업데이트 완료 - username: {}", userDto.getUserid());

            // 2. TB_XUSERS 테이블 업데이트
            TB_XUSERSId xusersId = new TB_XUSERSId(userDto.getCustcd(), userDto.getUserid());
            Optional<TB_XUSERS> xusersOptional = tbxusersService.findById(xusersId);

            if (xusersOptional.isPresent()) {
                TB_XUSERS xusers = xusersOptional.get();
                xusers.setPernm(userDto.getFirst_name() != null ? userDto.getFirst_name() : xusers.getPernm());
                xusers.setPasswd1(userDto.getPw() != null ? userDto.getPw() : xusers.getPasswd1());
                xusers.setPasswd2(userDto.getPw() != null ? userDto.getPw() : xusers.getPasswd2());

                tbxusersService.save(xusers);
                //log.info("TB_XUSERS 테이블 업데이트 완료 - username: {}", userDto.getUserid());
            } else {
                log.warn("TB_XUSERS 데이터를 찾을 수 없습니다 - username: {}, custcd: {}", userDto.getUserid(), userDto.getCustcd());
            }

            result.success = true;
            result.message = "사용자 정보가 성공적으로<br>변경되었습니다.";
        } else {
            result.success = false;
            result.message = "사용자를 찾을 수 없습니다.";
            log.error("사용자를 찾을 수 없습니다 - username: {}", userDto.getUserid());
        }

        return result;
    }



}
