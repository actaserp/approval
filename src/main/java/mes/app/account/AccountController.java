package mes.app.account;

import java.io.IOException;
import java.net.UnknownHostException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.transaction.Transactional;

import lombok.extern.slf4j.Slf4j;
import mes.app.MailService;
import mes.app.account.service.*;
import mes.app.system.service.AuthListService;
import mes.app.system.service.UserService;
import mes.domain.DTO.UserCodeDto;
import mes.domain.entity.*;
import mes.domain.entity.actasEntity.*;
import mes.domain.repository.*;
import mes.domain.repository.actasRepository.TB_XA012Repository;
import mes.domain.repository.actasRepository.TB_XClientRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.util.StringUtils;

import org.springframework.web.servlet.ModelAndView;

import mes.domain.model.AjaxResult;
import mes.domain.security.CustomAuthenticationToken;
import mes.domain.security.Pbkdf2Sha256;
import mes.domain.services.AccountService;
import mes.domain.services.SqlRunner;

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

	@Autowired
	private PasswordEncoder passwordEncoder;

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

		log.info("로그인 시도, username: {}", username);

		AjaxResult result = new AjaxResult();
		HashMap<String, Object> data = new HashMap<>();
		result.data = data;

		// 사용자 조회
		Optional<User> optionalUser = userRepository.findByUsername(username);
		if (optionalUser.isEmpty()) {
			data.put("code", "NOUSER");
			return result;
		}

		User user = optionalUser.get();
		String storedPassword = user.getPassword();

		// 비밀번호 검증
		if (!authenticate(password, storedPassword)) {
			data.put("code", "NOPW");
			return result;
		}

		// 인증 성공 처리
		UsernamePasswordAuthenticationToken authReq = new UsernamePasswordAuthenticationToken(username, password);
		CustomAuthenticationToken auth = (CustomAuthenticationToken) authManager.authenticate(authReq);

		if (auth != null) {
			data.put("code", "OK");

			try {
				accountService.saveLoginLog("login", auth);
			} catch (UnknownHostException e) {
				log.error("로그 저장 중 에러 발생", e);
			}

			// Spring Security 세션 설정
			SecurityContext sc = SecurityContextHolder.getContext();
			sc.setAuthentication(auth);

			HttpSession session = request.getSession(true);
			session.setAttribute("SPRING_SECURITY_CONTEXT", sc);
		} else {
			result.success = false;
			data.put("code", "NOID");
		}

		return result;
	}

	public boolean authenticate(String rawPassword, String storedPassword) {
		// 신규 방식 검증
		if (passwordEncoder.matches(rawPassword, storedPassword)) {
			return true;
		}

		// 기존 방식 검증
		if (Pbkdf2Sha256.verification(rawPassword, storedPassword)) {
			// 기존 방식 검증 성공 시 새로운 방식으로 저장
			String newEncodedPassword = passwordEncoder.encode(rawPassword);
			updatePasswordInDatabase(newEncodedPassword);
			return true;
		}

		// 두 방식 모두 실패
		return false;
	}

	private void updatePasswordInDatabase(String newPassword) {
		Optional<User> optionalUser = userRepository.findByUsername(
				SecurityContextHolder.getContext().getAuthentication().getName());
		if (optionalUser.isPresent()) {
			User user = optionalUser.get();
			user.setPassword(newPassword); // 새로운 비밀번호 설정
			userRepository.save(user);    // 변경 내용 저장
		}
	}







	@GetMapping("/account/myinfo")
	public AjaxResult getUserInfo(Authentication auth){
		User user = (User)auth.getPrincipal();
		AjaxResult result = new AjaxResult();

		Map<String, Object> dicData = userService.getUserInfo(user.getUsername());
		result.data = dicData;
		return result;
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
			@RequestParam(value = "password") String password
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

				// 비밀번호 해싱
				String hashedPassword = passwordEncoder.encode(password);

				// 사용자 저장
				User user = User.builder()
						.username(id)
						.password(hashedPassword)
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

				String currentDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));

				TB_XUSERS xusers = TB_XUSERS.builder()
						.passwd1(password)
						.passwd2(password)
						.shapass(hashedPassword)
						.pernm(name)
						.useyn("1")
						.domcls("%")
						.spjangcd(spjangcd)
						.upddate(currentDate)
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

	@PostMapping("/user-auth/searchAccount")
	public AjaxResult IdSearch(@RequestParam("usernm") final String usernm,
							   @RequestParam("mail") final String mail){

		AjaxResult result = new AjaxResult();

		// 사업자 번호와 대표자를 기반으로 사용자 검색
		List<String> user = userRepository.findByFirstNameAndEmailNative(usernm, mail);

		if (!user.isEmpty()) {
			result.success = true;
			result.data = user;
		} else {
			result.success = false;
			result.message = "해당 사용자가 존재하지 않습니다.";
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