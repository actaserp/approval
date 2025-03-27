package mes.app.system;

import java.sql.Timestamp;
import java.util.*;

import javax.servlet.http.HttpServletRequest;
import javax.transaction.Transactional;

import lombok.extern.slf4j.Slf4j;
import mes.app.PaymentList.service.PaymentListService;
import mes.app.UtilClass;
import mes.app.account.service.TB_XClientService;
import mes.app.account.service.TB_xusersService;
import mes.domain.entity.*;
import mes.domain.entity.actasEntity.*;
import mes.domain.repository.*;
import mes.domain.repository.actasRepository.TB_XA012Repository;
import mes.domain.repository.actasRepository.TB_xusersRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import mes.app.system.service.UserService;
import mes.domain.model.AjaxResult;
import mes.domain.security.Pbkdf2Sha256;
import mes.domain.services.CommonUtil;
import mes.domain.services.SqlRunner;

@Slf4j
@RestController
@RequestMapping("/api/system/user")
public class UserController {

	@Autowired
	private UserService userService;

	@Autowired
	UserRepository userRepository;

	@Autowired
	RelationDataRepository relationDataRepository;

	@Autowired
	SqlRunner sqlRunner;

	@Autowired
	private TB_RP945Repository tB_RP945Repository;

	@Autowired
	TB_xusersService xusersService;

	@Autowired
	TB_xusersRepository xusersRepository;

	@Autowired
	TB_XA012Repository tbXA012Repository;

	@Autowired
	JdbcTemplate jdbcTemplate;

	@Autowired
	private PaymentListService paymentListService;


	@GetMapping("/read")
	public AjaxResult getUserList(@RequestParam(value = "userGroup", required = false) String userGroup, // 사용자그룹
																@RequestParam(value = "name" ,required = false) String keyword, // 이름
								  Authentication auth){
		AjaxResult result = new AjaxResult();
		User user = (User)auth.getPrincipal();
		boolean superUser = user.getSuperUser();
		log.info("사용자 관리__ userGroup: {}, keyword: {}", userGroup, keyword);
		if (!superUser) {
			superUser = user.getUserProfile().getUserGroup().getCode().equals("dev");
		}

		List<Map<String, Object>> items = this.userService.getUserList(superUser, userGroup, keyword);

		result.data = items;

		return result;
	}

	@GetMapping("/detail")
	public AjaxResult getUserDetail(@RequestParam(value = "id", required = false) String id) {
		AjaxResult result = new AjaxResult();

		try {
			if (id != null && !id.isEmpty()) {
				// ID로 특정 사용자 정보 조회
				Map<String, Object> userDetail = userService.getUserDetailById(id);

				// fullAddress 분리하여 address1과 address2로 추가
				if (userDetail.containsKey("fullAddress")) {
					String fullAddress = (String) userDetail.get("fullAddress");
					Map<String, String> addressParts = splitAddress(fullAddress);
					userDetail.put("address1", addressParts.get("address1"));
					userDetail.put("address2", addressParts.get("address2"));
				}

				result.success = true;
				result.data = userDetail;
				result.message = "데이터 조회 성공";
			} else {
				result.success = false;
				result.message = "유효한 ID가 제공되지 않았습니다.";
			}
		} catch (Exception e) {
			result.success = false;
			result.message = "데이터를 가져오는 중 오류가 발생했습니다.";
		}

		return result;
	}

	private Map<String, String> splitAddress(String fullAddress) {
		Map<String, String> addressParts = new HashMap<>();
		if (fullAddress != null && !fullAddress.isEmpty()) {
			String[] parts = fullAddress.split("\\|", 2); // "|"를 기준으로 분리
			addressParts.put("address1", parts[0].trim()); // 첫 번째 부분
			addressParts.put("address2", parts.length > 1 ? parts[1].trim() : ""); // 두 번째 부분
		} else {
			addressParts.put("address1", "");
			addressParts.put("address2", "");
		}
		return addressParts;
	}

	// 사용자 그룹 조회
	@GetMapping("/user_grp_list")
	public AjaxResult getUserGrpList(
			@RequestParam(value="id") Integer id,
			HttpServletRequest request) {

		List<Map<String, Object>> items = this.userService.getUserGrpList(id);
		AjaxResult result = new AjaxResult();
		result.data = items;
		return result;
	}

	@PostMapping("/save")
	@Transactional
	public AjaxResult saveUser(
			@RequestParam(value = "id", required = false) Integer id,
			@RequestParam(value = "userid") String userid,
			@RequestParam(value = "is_active") boolean isActive,
			@RequestParam(value = "password") String password,
			@RequestParam(value = "UserGroup_id", required = false) Integer UserGroup_id,
			@RequestParam(value = "first_name", required = false ) String first_name,
			@RequestParam(value = "perid", required = false) String perid,
			@RequestParam(value = "divinm", required = false) String divinm,
			@RequestParam(value = "rspnm", required = false) String rspnm,
			@RequestParam(value = "spjangcd", required = false) String spjangcd,
			Authentication auth
	) {
		AjaxResult result = new AjaxResult();
		try {
			System.out.println("저장 로직 시작");

			boolean isNewUser = (id == null); // 새 사용자 여부 판단
			User user;


			if (isNewUser) {
				// 중복된 아이디 확인
				if (userRepository.findByUsername(userid).isPresent()) {
					result.success = false;
					result.message = "중복된 아이디가 존재합니다.";
					return result;
				}
				user = User.builder()
						.username(userid)
						.password(Pbkdf2Sha256.encode(password))
						.first_name(first_name)
						.last_name(first_name)
						.active(true)
						.is_staff(false)
						.date_joined(new Timestamp(System.currentTimeMillis()))
						.superUser(false)
						.agencycd(perid)
						.spjangcd(spjangcd)
						.build();
			} else {
				// 기존 사용자 업데이트
				user = userRepository.findById(id)
						.orElseThrow(() -> new RuntimeException("해당 사용자가 없습니다."));
				user.setPassword(Pbkdf2Sha256.encode(password));
				user.setFirst_name(first_name);
				user.setLast_name(first_name);
				user.setActive(isActive);
				user.setAgencycd(perid);
				user.setSpjangcd(spjangcd);
			}

			// 사용자 저장
			user = userRepository.save(user);
			System.out.println(isNewUser ? "새 사용자 저장 완료: " : "기존 사용자 업데이트 완료: " + user.getUsername());

			// 사용자 프로필 처리
			String profileSql = """
			SET IDENTITY_INSERT user_profile ON;
		
			MERGE INTO user_profile AS target
			USING (SELECT ? AS _created, ? AS lang_code, ? AS Name, ? AS UserGroup_id, ? AS User_id) AS source
			ON target.User_id = source.User_id
			WHEN MATCHED THEN
				UPDATE SET Name = source.Name, UserGroup_id = source.UserGroup_id
			WHEN NOT MATCHED THEN
				INSERT (_created, lang_code, Name, UserGroup_id, User_id)
				VALUES (source._created, source.lang_code, source.Name, source.UserGroup_id, source.User_id);
		
			SET IDENTITY_INSERT user_profile OFF;
			""";

			jdbcTemplate.update(
					profileSql,
					new Timestamp(System.currentTimeMillis()), // _created
					"ko-KR",                                  // lang_code
					first_name,                                    // Name
					UserGroup_id,                             // UserGroup_id
					user.getId()                              // User_id
			);

			System.out.println("User Profile 저장 또는 업데이트 완료");



			// 거래처 처리 로직
			String custcd = "SWSPANEL";
			List<String> spjangcds = Arrays.asList("ZZ", "YY");

			List<TB_XA012> tbX_A012List = tbXA012Repository.findByCustcdAndSpjangcds(custcd, spjangcds);
			if (tbX_A012List.isEmpty()) {
				result.success = false;
				result.message = "custcd 및 spjangcd에 해당하는 데이터를 찾을 수 없습니다.";
				return result;
			}
			String maxCltcd = xusersRepository.findMaxCltcd();
			String newCltcd = generateNewCltcd(maxCltcd);

			// 거래처 데이터 확인 및 저장
			Optional<TB_XUSERS> existingClientOpt = xusersRepository.findByIdUserid(userid);
			TB_XUSERS tbXusers;

			if (existingClientOpt.isPresent()) {
				tbXusers = existingClientOpt.get();
				tbXusers.setPernm(first_name);
				tbXusers.setPerid(perid);
				tbXusers.setSpjangcd(spjangcd);
				tbXusers.setUseyn(String.valueOf(isActive ? 1 : 0));
			} else {
				tbXusers = TB_XUSERS.builder()
						.pernm(first_name)
						.id(new TB_XUSERSId(custcd, newCltcd))
						.passwd2(password)
						.passwd1(password)
						.perid(perid)
						.spjangcd(spjangcd)
						.build();
			}

			xusersService.save(tbXusers);
			System.out.println("TB_XCLIENT 저장 완료");

			result.success = true;
			result.message = isNewUser ? "사용자가 성공적으로 등록되었습니다." : "사용자 정보가 성공적으로 업데이트되었습니다.";
		} catch (Exception e) {
			System.err.println("오류 발생: " + e.getMessage());
			e.printStackTrace();
			result.success = false;
			result.message = "사용자 정보를 저장하는 중 오류가 발생했습니다.";
		}
		return result;
	}

	// 새로운 cltcd 생성 메서드
	private String generateNewCltcd(String maxCltcd) {
		int newNumber = 1; // 기본값
		// 최대 cltcd 값이 null이 아니고 "SW"로 시작하는 경우
		if (maxCltcd != null && maxCltcd.startsWith("SW")) {
			String numberPart = maxCltcd.substring(2); // "SW"를 제외한 부분
			newNumber = Integer.parseInt(numberPart) + 1; // 숫자 증가
		}
		// 새로운 cltcd 생성: "SW" 접두사와 5자리 숫자로 포맷
		return String.format("SW%05d", newNumber);
	}
	@GetMapping("/check")
	public ResponseEntity<Map<String, Boolean>> checkUserExists(@RequestParam(value = "id") Integer id) {
		boolean exists = userRepository.findById(id).isPresent();
		Map<String, Boolean> response = new HashMap<>();
		response.put("exists", exists);
		return ResponseEntity.ok(response);
	}

	// user 삭제
	@Transactional
	@PostMapping("/delete")
	public AjaxResult deleteUser(@RequestParam("id") String id,
								 @RequestParam(value = "username", required = false) String username,
								 @RequestParam boolean auth) {
		AjaxResult result = new AjaxResult();
		System.out.println("삭제 들어옴");

		if(auth){
			result.success = false;
			result.message = "슈퍼유저는 수정 및 삭제가 불가능합니다.";
			return result;
		}
		System.out.println("조회하려는 username: " + UtilClass.removeBrackers(username));
		Optional<User> user = userRepository.findByUsername(UtilClass.removeBrackers(username));
		if (user.isPresent()) {
			System.out.println("User 조회 성공: " + user.get());
			xusersRepository.deleteById(user.get().getUsername());
		} else {
			System.out.println("User 조회 실패. username이 존재하지 않습니다.");
		}

		Integer userid  = Integer.parseInt(UtilClass.removeBrackers(id));
		this.userRepository.deleteById(userid);

		return result;
	}

	@GetMapping("/checkUserId")
	public ResponseEntity<Map<String, Boolean>> checkUserId(@RequestParam String userid) {
		Map<String, Boolean> response = new HashMap<>();
		boolean exists = userService.isUserIdExists(userid); // username 중복 체크
		response.put("exists", exists);
		return ResponseEntity.ok(response);
	}


	@Transactional
	@PostMapping("/activate")
	public AjaxResult Activate(@RequestParam Integer id,
							   @RequestParam boolean is_active
	){

		AjaxResult result = new AjaxResult();
		Optional<User> user = userRepository.findById(id);
		if(user.isPresent())
		{
			User u = user.get();
			if(u.getSuperUser()){
				result.success = false;
				result.message = "슈퍼유저는 변경이 불가능합니다.";

			}else{
				//save호출
				u.setActive(is_active);
				result.success = true;
				result.message = "변경되었습니다.";
			}
		}else{
			result.success = false;
			result.message = "해당 사용자가 존재하지 않습니다.";

		}
		return result;
	}

	@PostMapping("/modfind")
	public AjaxResult getBtId(@RequestBody String userid){

		///userid = new UtilClass().removeBrackers(userid);
		AjaxResult result = new AjaxResult();

		List<TB_RP945> tbRp945 =  tB_RP945Repository.findByUserid(userid);

		if(!tbRp945.isEmpty()){
			result.success = true;
			result.data = tbRp945;
		}else{
			result.success = false;
			result.message = "해당 유저에 대한 권한상세정보가 없습니다.";
		}

		return result;
	}


	// user 패스워드 셋팅
	@PostMapping("/passSetting")
	@Transactional
	public AjaxResult userPassSetting(
			@RequestParam(value="id", required = false) Integer id,
			@RequestParam(value="pass1", required = false) String loginPwd,
			@RequestParam(value="pass2", required = false) String loginPwd2,
			Authentication auth
	) {

		User user = null;
		AjaxResult result = new AjaxResult();

		if (StringUtils.hasText(loginPwd)==false | StringUtils.hasText(loginPwd2)==false) {
			result.success=false;
			result.message="The verification password is incorrect.";
			return result;
		}

		if(loginPwd.equals(loginPwd2)==false) {
			result.success=false;
			result.message="The verification password is incorrect.";
			return result;
		}

		user = this.userRepository.getUserById(id);
		user.setPassword(Pbkdf2Sha256.encode(loginPwd));
		this.userRepository.save(user);

		return result;
	}

	@PostMapping("/save_user_grp")
	@Transactional
	public AjaxResult saveUserGrp(
			@RequestParam(value="id") Integer id,
			@RequestBody MultiValueMap<String,Object> Q,
			Authentication auth
	) {

		User user = (User)auth.getPrincipal();;

		AjaxResult result = new AjaxResult();

		List<Map<String, Object>> items = CommonUtil.loadJsonListMap(Q.getFirst("Q").toString());

		List<RelationData> rdList = this.relationDataRepository.findByDataPk1AndTableName1AndTableName2(id,"auth_user", "user_group");

		// 등록된 그룹 삭제
		for (int i = 0; i < rdList.size(); i++) {
			this.relationDataRepository.deleteById(rdList.get(i).getId());
		}

		this.relationDataRepository.flush();
		for (int i = 0; i< items.size(); i++) {

			String check = "";

			if (items.get(i).get("grp_check") != null) {
				check = items.get(i).get("grp_check").toString();
			}

			if (check.equals("Y")) {
				RelationData rd = new RelationData();
				rd.setDataPk1(id);
				rd.setTableName1("auth_user");
				rd.setDataPk2(Integer.parseInt(items.get(i).get("grp_id").toString()));
				rd.setTableName2("user_group");
				rd.setRelationName("auth_user-user_group");
				rd.setChar1("Y");
				rd.set_audit(user);

				this.relationDataRepository.save(rd);
			}
		}

		return result;
	}

	@GetMapping("/read2")
	public AjaxResult getPaymentList2(@RequestParam(value = "search_spjangcd", required = false) String spjangcd,
																		@RequestParam(value = "appnum", required = false) String appnum) {
		AjaxResult result = new AjaxResult();
		log.info("더블클릭(결재목록) 들어온 데이터:spjangcd {}, appnum: {} ", spjangcd, appnum);

		try {

			List<Map<String, Object>> getPaymentList2 = paymentListService.getUserinfo(spjangcd,appnum);

			for (Map<String, Object> item : getPaymentList2) {
				//날짜 포맷
				formatDateField(item, "appdate");
			}

			result.success = true;
			result.message = "데이터 조회 성공";
			result.data = getPaymentList2;
		} catch (Exception e) {
			// 예외 처리
			result.success = false;
			result.message = "데이터 조회 중 오류 발생: " + e.getMessage();
		}

		return result;
	}
	// 날짜 포맷
	private void formatDateField(Map<String, Object> item, String fieldName) {
		Object dateValue = item.get(fieldName);
		if (dateValue instanceof String) {
			String dateStr = (String) dateValue;
			try {
				if (dateStr.length() == 8) { // "yyyyMMdd" 형식인지 확인
					String formattedDate = dateStr.substring(0, 4) + "-" + dateStr.substring(4, 6) + "-" + dateStr.substring(6, 8);
					item.put(fieldName, formattedDate);
				} else {
					item.put(fieldName, "잘못된 날짜 형식");
				}
			} catch (Exception ex) {
				log.error("{} 변환 중 오류 발생: {}", fieldName, ex.getMessage());
				item.put(fieldName, "잘못된 날짜 형식");
			}
		}
	}

}
