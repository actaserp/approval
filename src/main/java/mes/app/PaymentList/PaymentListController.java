package mes.app.PaymentList;

import lombok.extern.slf4j.Slf4j;
import mes.app.PaymentList.service.PaymentListService;
import mes.domain.entity.User;
import mes.domain.entity.UserCode;
import mes.domain.model.AjaxResult;
import mes.domain.repository.UserCodeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/PaymentList")
public class PaymentListController { //결재목록

  @Autowired
  private UserCodeRepository userCodeRepository;

  @Autowired
  private PaymentListService paymentListService;

  @GetMapping("/read")
  public AjaxResult getPaymentList(@RequestParam(value = "startDate") String startDate,
                                   @RequestParam(value = "endDate") String endDate,
                                   @RequestParam(value = "search_spjangcd", required = false) String spjangcd,
                                   @RequestParam(value = "SearchPayment", required = false) String SearchPayment,
                                   @RequestParam(value = "searchUserNm", required = false) String searchUserNm,
                                   Authentication auth) {
    AjaxResult result = new AjaxResult();
    log.info("주문 확인 read 들어온 데이터:startDate{}, endDate{}, spjangcd {}, SearchPayment {} ,searchUserNm {} ", startDate, endDate, spjangcd, SearchPayment,searchUserNm);

    try {

      User user = (User) auth.getPrincipal();
      String agencycd = user.getAgencycd().replaceFirst("^p", "");
      // 데이터 조회
      List<Map<String, Object>> getPaymentList = paymentListService.getPaymentList(spjangcd, startDate, endDate, SearchPayment,searchUserNm, agencycd);

      for (Map<String, Object> item : getPaymentList) {
        //날짜 포맷 변환 (repodate)
        formatDateField(item, "repodate");
        //날짜 포맷 변환 (appdate)
        formatDateField(item, "appdate");

        //papercd 값이 "101"이면 "전표결재(지출결의서)"
        /*if ("101".equals(item.get("papercd"))) {
          item.put("papercd", "전표결재(지출결의서)");
        }*/
      }

      // 데이터가 있을 경우 성공 메시지
      result.success = true;
      result.message = "데이터 조회 성공";
      result.data = getPaymentList;

    } catch (Exception e) {
      // 예외 처리
      result.success = false;
      result.message = "데이터 조회 중 오류 발생: " + e.getMessage();
    }

    return result;
  }

  @GetMapping("/read1")
  public AjaxResult getPaymentList1(@RequestParam(value = "startDate") String startDate,
                                   @RequestParam(value = "endDate") String endDate,
                                   @RequestParam(value = "search_spjangcd", required = false) String spjangcd,
                                   Authentication auth) {
    AjaxResult result = new AjaxResult();
    log.info("결재목록_문서현황 read 들어온 데이터:startDate{}, endDate{}, spjangcd {} ", startDate, endDate, spjangcd);

    try {

      User user = (User) auth.getPrincipal();
      String agencycd = user.getAgencycd().replaceFirst("^p", "");
      String userName = user.getFirst_name();
      // 데이터 조회
      List<Map<String, Object>> getPaymentList = paymentListService.getPaymentList1(spjangcd, startDate, endDate, agencycd);


      // 데이터가 있을 경우 성공 메시지
      result.success = true;
      result.message = "데이터 조회 성공";
      result.data = Map.of(
          "userName", userName,  // 사용자 이름
          "paymentList", getPaymentList // 결재 목록 리스트
      );

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

  @GetMapping("/payType")
  public AjaxResult ordFlagType(
      @RequestParam(value = "parentCode", required = false) String parentCode) {
    AjaxResult result = new AjaxResult();

    try {
      // parentCode를 기준으로 하위 그룹 필터링
      List<UserCode> data = (parentCode != null)
          ? userCodeRepository.findByParentId(userCodeRepository.findByCode(parentCode).stream().findFirst().get().getId())
          : userCodeRepository.findAll();

      // 성공 시 데이터와 메시지 설정
      result.success = true;
      result.message = "데이터 조회 성공";
      result.data = data;

    } catch (Exception e) {
      // 예외 발생 시 처리
      result.success = false;
      result.message = "데이터 조회 중 오류 발생: " + e.getMessage();
    }

    return result;
  }

  @GetMapping("/bindSpjangcd")
  public AjaxResult bindSpjangcd(Authentication auth) {
    // 관리자 사용가능 페이지 사업장 코드 선택 로직
    User user = (User) auth.getPrincipal();
    String username = user.getUsername();
    String spjangcd = paymentListService.getSpjangcd(username, "");
    // 사업장 코드 선택 로직 종료 반환 spjangcd 활용
    AjaxResult result = new AjaxResult();
    result.data = spjangcd;
    return result;
  }


}
