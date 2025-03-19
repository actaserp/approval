package mes.app.account;

import lombok.extern.slf4j.Slf4j;
import mes.app.MailService;
import mes.app.account.service.TB_XClientService;
import mes.app.account.service.TB_xusersService;
import mes.app.system.service.UserService;
import mes.domain.DTO.UserCodeDto;
import mes.domain.DTO.UserDto;
import mes.domain.entity.User;
import mes.domain.entity.UserCode;
import mes.domain.entity.UserGroup;
import mes.domain.entity.actasEntity.TB_XCLIENT;
import mes.domain.entity.actasEntity.TB_XUSERS;
import mes.domain.entity.actasEntity.TB_XUSERSId;
import mes.domain.model.AjaxResult;
import mes.domain.repository.UserCodeRepository;
import mes.domain.repository.UserGroupRepository;
import mes.domain.repository.UserRepository;
import mes.domain.repository.actasRepository.TB_XClientRepository;
import mes.domain.security.CustomAuthenticationToken;
import mes.domain.security.Pbkdf2Sha256;
import mes.domain.services.AccountService;
import mes.domain.services.SqlRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.transaction.Transactional;
import java.io.IOException;
import java.net.UnknownHostException;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Slf4j
@RestController
public class AccountController {

	@Autowired
	AccountService accountService;

	@Autowired
	UserRepository userRepository;

	@Autowired
	UserCodeRepository userCodeRepository;

	@Autowired
	SqlRunner sqlRunner;

	@Autowired
	MailService emailService;

	@Autowired
	TB_xusersService tbxusersService;
	@Autowired
	TB_XClientService tbXClientService;
	@Autowired
	TB_XClientRepository tbXClientRepository;
	@Autowired
	JdbcTemplate jdbcTemplate;

	@Resource(name="authenticationManager")
	private AuthenticationManager authManager;
	@Autowired
	private UserGroupRepository userGroupRepository;

	private Boolean flag;
	private Boolean flag_pw;

	private final ConcurrentHashMap<String, String> tokenStore = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<String, Long> tokenExpiry = new ConcurrentHashMap<>();

	@Autowired
    private UserService userService;


	@GetMapping("/login")
	public ModelAndView loginPage(
			HttpServletRequest request,
			HttpServletResponse response,
			HttpSession session, Authentication auth) {


		// User-Agent를 기반으로 모바일 여부 감지
		String userAgent = request.getHeader("User-Agent").toLowerCase();
		boolean isMobile = userAgent.contains("mobile") || userAgent.contains("android") || userAgent.contains("iphone");

		// 모바일이면 "mlogin" 뷰로, 아니면 "login" 뷰로 설정
		ModelAndView mv = new ModelAndView(isMobile ? "mlogin" : "login");

		Map<String, Object> userInfo = new HashMap<String, Object>();
		Map<String, Object> gui = new HashMap<String, Object>();

		mv.addObject("userinfo", userInfo);

		mv.addObject("gui", gui);
		if(auth!=null) {
			SecurityContextLogoutHandler handler =  new SecurityContextLogoutHandler();
			handler.logout(request, response, auth);
		}

		return mv;
	}

	@GetMapping("/logout")
	public void logout(
			HttpServletRequest request
			, HttpServletResponse response) throws IOException {

		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		SecurityContextLogoutHandler handler =  new SecurityContextLogoutHandler();

		this.accountService.saveLoginLog("logout", auth);

		handler.logout(request, response, auth);
		response.sendRedirect("/login");
	}

	@PostMapping("/login")
	public AjaxResult postLogin(
			@RequestParam("username") final String username,
			@RequestParam("password") final String password,
			final HttpServletRequest request) throws UnknownHostException {

		log.info("로그인 요청 - username: {}, password: {}", username, password);

		AjaxResult result = new AjaxResult();
		HashMap<String, Object> data = new HashMap<>();
		result.data = data;

		UsernamePasswordAuthenticationToken authReq = new UsernamePasswordAuthenticationToken(username, password);
		CustomAuthenticationToken auth = null;

		try {
			log.info("인증 시작 - authManager.authenticate() 호출");
			auth = (CustomAuthenticationToken) authManager.authenticate(authReq);
			log.info("인증 성공 - 사용자 정보: {}", auth.getPrincipal());
		} catch (AuthenticationException e) {
			log.error("인증 실패 - 예외 발생: {}", e.getMessage(), e);
			data.put("code", "NOUSER");
			return result;
		}

		if (auth != null) {
			User user = (User) auth.getPrincipal();
			log.info("사용자 로그인 성공 - username: {}, active 상태: {}", user.getUsername(), user.getActive());

			data.put("code", "OK");

			try {
				log.info("로그인 로그 저장 시작");
				this.accountService.saveLoginLog("login", auth);
				log.info("로그인 로그 저장 완료");
			} catch (UnknownHostException e) {
				log.error("로그인 로그 저장 중 오류 발생: {}", e.getMessage(), e);
			}
		} else {
			log.warn("인증 객체 null - 로그인 실패");
			result.success = false;
			data.put("code", "NOID");
		}

		log.info("SecurityContext 설정");
		SecurityContext sc = SecurityContextHolder.getContext();
		sc.setAuthentication(auth);

		HttpSession session = request.getSession(true);
		session.setAttribute("SPRING_SECURITY_CONTEXT", sc);

		return result;
	}

	//내정보 불러오기
	@GetMapping("/account/myinfo")
	public AjaxResult getUserInfo(Authentication auth) {
		AjaxResult result = new AjaxResult();

		try {
			User user = (User) auth.getPrincipal();
			String username = user.getUsername();

			Map<String, Object> userInform = userService.getUserInform(username);

			if (userInform == null) {
				result.success = false;
				result.data = "사용자 정보를 찾을 수 없습니다.";
			} else {
				result.success = true;
				result.data = userInform;
			}
		} catch (Exception e) {
			result.success = false;
			result.data = "시스템 에러가 발생했습니다.";
		}

		return result;
	}


	@PostMapping("/account/myinfosave")
	public AjaxResult setUserInfo(@RequestBody UserDto userDto){
		AjaxResult result = new AjaxResult();
		log.info("받은 요청 데이터: {}", userDto);
		if(userDto.getPw() != null && !userDto.getPw().isEmpty()){
			if(!userDto.getPw().equals(userDto.getPw2())){
				result.success = false;
				result.message = "입력하신 비밀번호가 맞지 않습니다.";
				log.error("비밀번호 불일치 - pw: {}, pw2: {}", userDto.getPw(), userDto.getPw2());
				return result;
			}
		}

		result = userService.updateUserInfo(userDto);
		log.info("사용자 정보 업데이트 결과: success={}, message={}", result.success, result.message);

		return result;
	}

	@GetMapping("/account/userinfo")
	@ResponseBody
	public AjaxResult getUserInfos(Authentication auth) {
		AjaxResult result = new AjaxResult();

		try {
			User user = (User) auth.getPrincipal();
			String username = user.getFirst_name();

			Map<String, String> data = new HashMap<>();
			data.put("username", username);

			result.success=(true);
			result.data = (data);
		} catch (Exception e) {
			result.success = (false);
			result.message = ("사용자 정보를 가져오는 중 오류가 발생했습니다.");
		}

		return result; // JSON 형식으로 반환
	}



	@PostMapping("/account/myinfo/password_change")
	public AjaxResult userPasswordChange(
			@RequestParam("name") final String name,
			@RequestParam("loginPwd") final String loginPwd,
			@RequestParam("loginPwd2") final String loginPwd2,
			Authentication auth
	) {

		User user = (User)auth.getPrincipal();
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

		user.setPassword(Pbkdf2Sha256.encode(loginPwd2));
		//user.getUserProfile().setName(name);
		this.userRepository.save(user);

		String sql = """
			UPDATE user_profile
			SET Name = :name,
				_modified = GETDATE(), 
				_modifier_id = :id
			WHERE id = :id
		""";

		MapSqlParameterSource dicParam = new MapSqlParameterSource();
		dicParam.addValue("name", name);
		dicParam.addValue("id", user.getId());
		this.sqlRunner.execute(sql, dicParam);
		return result;
	}


	/***
	 *  아이디 중복 확인
	 * **/
	@PostMapping("/useridchk")
	public AjaxResult IdChk(@RequestParam("userid") final String userid){

		AjaxResult result = new AjaxResult();


		Optional<User> user = userRepository.findByUsername(userid);


		if(!user.isPresent()){

			result.success = true;
			result.message = "사용할 수 있는 계정입니다.";
			return result;

		}else {
			result.success = false;
			result.message = "중복된 계정이 존재합니다.";
			return result;
		}


	}

	//사용자 신청()
	@PostMapping("/Register/save")
	@Transactional
	public AjaxResult RegisterUser(
			@RequestParam(value = "phone", required = false) String phone,
			@RequestParam(value = "email", required = false) String email,
			@RequestParam(value = "id") String id,
			@RequestParam(value = "name") String name,
			@RequestParam(value = "password") String password,
			@RequestParam(value = "add") String addess
	) {
		log.info("phone: {}, email: {}, id: {}, name: {}, password: {}", phone, email, id, name, password);
		AjaxResult result = new AjaxResult();

		try {
			if (flag) {
				// "ZZ" 값을 전달하여 호출
				List<Map<String, Object>> results = userService.getCustcdAndSpjangcd("ZZ");

				if (results.isEmpty()) {
					System.out.println("No data found for spjangcd = 'ZZ'");
				} else {
					results.forEach(row -> {
						System.out.println("custcd: " + row.get("custcd"));
						System.out.println("spjangcd: " + row.get("spjangcd"));
					});
				}
				// 첫 번째 조회된 데이터 사용
				Map<String, Object> firstRow = results.get(0);
				String custcd = (String) firstRow.get("custcd");
				String spjangcd = (String) firstRow.get("spjangcd");

				// 사용자 저장
				User user = User.builder()
						.username(id)
						.password(Pbkdf2Sha256.encode(password))
						.phone(phone)
						.email(email)
						.first_name(name)
						.last_name(name)
						.tel("")
						.spjangcd(spjangcd)
						.active(true)
						.is_staff(false)
						.date_joined(new Timestamp(System.currentTimeMillis()))
						.superUser(false)
						.build();
				userService.save(user); // User 저장

				jdbcTemplate.execute("SET IDENTITY_INSERT user_profile ON");
				// UserProfile 저장 (JDBC 사용)
				String sql = "INSERT INTO user_profile (_created, lang_code, Name, UserGroup_id, User_id) VALUES (?,?, ?, ?, ?)";
				jdbcTemplate.update(sql,
						new Timestamp(System.currentTimeMillis()), // 현재 시간
						"ko-KR", // lang_code (예: 한국어)
						name, // Name (사용자 이름)
						35 ,// UserGroup_id (일반거래처)
						user.getId() // User_id
				);
				jdbcTemplate.execute("SET IDENTITY_INSERT user_profile OFF");

				TB_XUSERS xusers = TB_XUSERS.builder()
						.passwd1(password)
						.passwd2(password)
						.pernm(name)
						.useyn("1")
						.spjangcd(spjangcd)
						.id(new TB_XUSERSId(custcd, id))
						.build();

				tbxusersService.save(xusers);


				result.success = true;
				result.message = "등록이 완료되었습니다.";
				flag = false;
			} else {
				result.success = false;
				result.message = "코드가 인증되지 않았습니다.";
			}
		} catch (Exception e) {
			result.success = false;
			System.out.println("오류 발생: " + e.getMessage());
			System.out.println(e);
		}

		return result;
	}

	
	@PostMapping("/account/updateUserInfo")
	@Transactional
	public AjaxResult updateUserInfo(@RequestBody Map<String, Object> userData) {
		System.out.println("받은 사용자 정보: " + userData);
		AjaxResult result = new AjaxResult();

		try {
			// 필수값 검증
			if (!userData.containsKey("login_id") || userData.get("login_id") == null) {
				result.success = false;
				result.message = "ID가 누락되었습니다.";
				return result;
			}

			// 사용자 정보 업데이트 처리
			String loginId = userData.get("login_id").toString();
			Optional<User> userOptional = userRepository.findByUsername(loginId);

			if (userOptional.isPresent()) {
				User user = userOptional.get();

				// 사용자 정보 업데이트
				if (user.getUserProfile() != null) {
					user.getUserProfile().setName(userData.getOrDefault("prenm", "").toString());
				}
				user.setEmail(userData.getOrDefault("email", "").toString());
				user.setPhone(userData.getOrDefault("phone", "").toString());
				user.setTel(userData.getOrDefault("tel", "").toString());

				// 저장
				userRepository.save(user);
			} else {
				result.success = false;
				result.message = "사용자를 찾을 수 없습니다.";
				return result;
			}

			// TB_XCLIENT 정보 업데이트
			Optional<TB_XCLIENT> clientOptional = tbXClientRepository.findBySaupnum(loginId);
			if (clientOptional.isPresent()) {
				TB_XCLIENT client = clientOptional.get();

				client.setCltnm(userData.getOrDefault("cltnm", "").toString());
				client.setBiztypenm(userData.getOrDefault("biztypenm", "").toString());
				client.setBizitemnm(userData.getOrDefault("bizitemnm", "").toString());
				client.setZipcd(userData.getOrDefault("postno", "").toString());

				// 주소 병합
				String address1 = userData.getOrDefault("address1", "").toString();
				String address2 = userData.getOrDefault("address2", "").toString();
				String fullAddress = address1 + (address2.isEmpty() ? "" : " | " + address2);
				client.setCltadres(fullAddress);
				// 저장
				tbXClientRepository.save(client);
			}

			if (userData.containsKey("User_id") && userData.containsKey("Name")) {
				String userId = userData.get("User_id").toString();
				String name = userData.get("Name").toString();

				String updateSql = """
                UPDATE user_profile
                SET 
                    Name = :name,
                    _modified = GETDATE()
                WHERE User_id = :userId;
            """;

				MapSqlParameterSource params = new MapSqlParameterSource();
				params.addValue("name", name);
				params.addValue("userId", userId);

				int rowsAffected = executeUpdate(updateSql, params);
				System.out.println("업데이트된 행 수: " + rowsAffected);
			}

			result.success = true;
			result.message = "사용자 정보가 성공적으로 업데이트되었습니다.";
		} catch (Exception e) {
			result.success = false;
			result.message = "정보 업데이트 중 오류가 발생했습니다.";
			e.printStackTrace();
		}

		return result;
	}
	public int executeUpdate(String sql, MapSqlParameterSource params) {
		try {
			return jdbcTemplate.update(sql, params);
		} catch (Exception e) {
			e.printStackTrace();
			return 0; // 실패 시 0 반환
		}
	}


	// 등록 확인
	@PostMapping("/user-auth/searchAccount")
	public AjaxResult IdSearch(@RequestParam(value = "usernm", required = false) String usernm,   // 이름
														 @RequestParam(value = "findUserid", required = false) String findUserid) { // 아이디

		AjaxResult result = new AjaxResult();

		// 입력값 검증
		if (usernm == null || usernm.trim().isEmpty() || findUserid == null || findUserid.trim().isEmpty()) {
			result.success = false;
			result.message = "이름과 아이디를 모두 입력해야 합니다.";
			return result;
		}

		log.info("사용자 검색 시작 - usernm: {}, findUserid: {}", usernm, findUserid);

		// 사용자 검색
		List<String> users = userRepository.findByFirstNameAndEmail(usernm, findUserid);

		if (!users.isEmpty()) {
			result.success = true;
			result.data = users;
			log.info("사용자 검색 성공 - {}개의 사용자 발견", users.size());
		} else {
			result.success = false;
			result.message = "해당 사용자가 존재하지 않습니다.";
			log.warn("사용자 검색 실패 - 검색 결과 없음");
		}

		return result;
	}


	@PostMapping("/user-auth/AuthenticationEmail")
	public AjaxResult PwSearch(@RequestParam("usernm") final String usernm,
							   @RequestParam("mail") final String mail){

		AjaxResult result = new AjaxResult();

		if(usernm.equals("empty")){
			sendEmailLogic(mail, "신규사용자");

			result.success = true;
			result.message = "인증 메일이 발송되었습니다.";
			return result;
		}

		int exists = userRepository.existsByUsernameAndEmail(usernm, mail);
		boolean flag = exists > 0;

		if(flag) {
			sendEmailLogic(mail, usernm);

			result.success = true;
			result.message = "인증 메일이 발송되었습니다.";
		}else {
			result.success = false;
			result.message = "해당 사용자가 존재하지 않습니다.";
		}

		return result;
	}


	private void sendEmailLogic(String mail, String usernm){
		Random random = new Random();
		int randomNum = 100000 + random.nextInt(900000); // 100000부터 999999까지의 랜덤 난수 생성
		String verificationCode = String.valueOf(randomNum); // 정수를 문자열로 변환
		emailService.sendVerificationEmail(mail, usernm, verificationCode);

		tokenStore.put(mail, verificationCode);
		tokenExpiry.put(mail, System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(3));

	}

	@PostMapping("/user-auth/SaveAuthenticationEmail")
	public AjaxResult saveMail(@RequestParam("usernm") final String usernm,
							   @RequestParam("name") final String name,
							   @RequestParam("mail") final String mail) {

		AjaxResult result = new AjaxResult();

		if (usernm.equals("empty")) {
			saveEmailLogic(mail, name);

			result.success = true;
			result.message = "인증 메일이 발송되었습니다.";
			return result;
		}

		int exists = userRepository.existsByUsernameAndEmail(usernm, mail);
		boolean flag = exists > 0;

		if (flag) {
			saveEmailLogic(mail, usernm);

			result.success = true;
			result.message = "인증 메일이 발송되었습니다.";
		} else {
			result.success = false;
			result.message = "해당 사용자가 존재하지 않습니다.";
		}

		return result;
	}


	private void saveEmailLogic(String mail, String usernm){
		Random random = new Random();
		int randomNum = 100000 + random.nextInt(900000); // 100000부터 999999까지의 랜덤 난수 생성
		String verificationCode = String.valueOf(randomNum); // 정수를 문자열로 변환
		emailService.saveVerificationEmail(mail, usernm, verificationCode);

		tokenStore.put(mail, verificationCode);
		tokenExpiry.put(mail, System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(3));

	}

	@PostMapping("/user-auth/verifyCode")
	public AjaxResult verifyCode(@RequestParam("code") final String code,
								 @RequestParam("mail") final String mail,
								 @RequestParam("password") final String password,
								 @RequestParam("userid") final String userid
	){

		AjaxResult result = new AjaxResult();
		result = verifyAuthenticationCode(code, mail);

		if(result.success){
			String pw = Pbkdf2Sha256.encode(password);


			userRepository.PasswordChange(pw, userid);


			result.success = true;
			result.message = "비밀번호가 변경되었습니다.";

			return result;
		}else{
			return result;
		}

		/*AjaxResult result = new AjaxResult();


		String storedToken = tokenStore.get(mail);

		if(storedToken != null && storedToken.equals(code)){
			long expiryTime = tokenExpiry.getOrDefault(mail, 0L);
			if(System.currentTimeMillis() > expiryTime){
				result.success = false;
				result.message = "인증 코드가 만료되었습니다.";
				tokenStore.remove(mail);
				tokenExpiry.remove(mail);
			} else {

				String pw = Pbkdf2Sha256.encode(password);


				userRepository.PasswordChange(pw, userid);


				result.success = true;
				result.message = "비밀번호가 변경되었습니다.";
			}
		}else {
			result.success = false;
			result.message = "인증 코드가 유효하지 않습니다.";
		}*/


	}

	private AjaxResult verifyAuthenticationCode(String code, String mail){

		AjaxResult result = new AjaxResult();

		String storedToken = tokenStore.get(mail);
		if(storedToken != null && storedToken.equals(code)){
			long expiryTime = tokenExpiry.getOrDefault(mail, 0L);
			if(System.currentTimeMillis() > expiryTime){
				result.success = false;
				result.message = "인증 코드가 만료되었습니다.";
				tokenStore.remove(mail);
				tokenExpiry.remove(mail);
			} else {
				result.success = true;
				result.message = "비밀번호가 변경되었습니다.";
			}
		}else{
			result.success = false;
			result.message = "인증 코드가 유효하지 않습니다.";
		}
		return result;
	}


	@GetMapping("/user-codes/parent")
	public List<UserCodeDto> getUserCodeByParentId(@RequestParam Integer parentId){

		List<UserCode> list = userCodeRepository.findByParentId(parentId);

		List<UserCodeDto> dtoList = list.stream()
				.map(userCode -> new UserCodeDto(
						userCode.getId(),
						userCode.getCode(),
						userCode.getValue()
				))
				.toList();

		return dtoList;
	}

	@GetMapping("/user-auth/type")
	public List<UserGroup> getUserAuthTypeAll(){
		return userGroupRepository.findAll();

	}

	@GetMapping("/user-codes/code")
	public List<UserCodeDto> getUserCodesByCode(@RequestParam Integer code) {

		List<UserCode> list = userCodeRepository.findByParentId(code);

		List<UserCodeDto> dtoList = list.stream()
				.map(userCode -> new UserCodeDto(
						userCode.getId(),
						userCode.getCode(),
						userCode.getValue()
				)).toList();

		return dtoList;
	}

	@PostMapping("/authentication")
	public AjaxResult Authentication(@RequestParam(value = "AuthenticationCode") String AuthenticationCode,
									 @RequestParam(value = "email", required = false) String email,
									 @RequestParam String type
	){

		AjaxResult result = verifyAuthenticationCode(AuthenticationCode, email);

		if(type.equals("new")){
			if(result.success){
				flag = true;
				result.message = "인증되었습니다.";

			}

		}else{
			if(result.success){
				flag_pw = true;
				result.message = "인증되었습니다.";
			}
		}

		return result;
	}

}