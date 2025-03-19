package mes.app.dashboard;

import mes.app.dashboard.service.DashBoardService2;
import mes.domain.entity.User;
import mes.domain.model.AjaxResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard2")
public class DashBoardController2 {
    @Autowired
    private DashBoardService2 dashBoardService2;

    // 작년 1월1일부터 작년오늘날자까지 상태별 건수
    @GetMapping("/LastYearCnt")
    private AjaxResult LastYearCnt(@RequestParam(value = "search_spjangcd") String search_spjangcd
                                    , Authentication auth) {
        // 관리자 사용가능 페이지 사업장 코드 선택 로직
        User user = (User) auth.getPrincipal();
        String username = user.getUsername();
        String spjangcd = dashBoardService2.getSpjangcd(username, search_spjangcd);
        String perid = dashBoardService2.getPerid(username);
        String splitPerid = perid.replaceFirst("p", ""); // ✅ 첫 번째 "p"만 제거
        // 올해 진행구분(appgubun)별 데이터 개수
        List<Map<String, Object>> ThisYearCnt = this.dashBoardService2.ThisYearCnt(spjangcd, splitPerid);

        // 결재요청받은(일별) 데이터
        List<Map<String, Object>> ThisMonthResCntOfDate = this.dashBoardService2.ThisMonthResCntOfDate(spjangcd, splitPerid);
        // 결재요청받은(월별) 데이터
        List<Map<String, Object>> ThisYearResCntOfMonth = this.dashBoardService2.ThisYearResCntOfMonth(spjangcd, splitPerid);
        // 결재 올린(일별) 데이터 개수
        List<Map<String, Object>> ThisMonthReqCntOfDate = this.dashBoardService2.ThisMonthReqCntOfDate(spjangcd, splitPerid);
        // 결재 올린(월별) 데이터 개수
        List<Map<String, Object>> ThisYearReqCntOfMonth = this.dashBoardService2.ThisYearReqCntOfMonth(spjangcd, splitPerid);

        AjaxResult result = new AjaxResult();
        Map<String, Object> items = new HashMap<String, Object>();
        items.put("ThisYearCnt", ThisYearCnt);
        items.put("ThisMonthResCntOfDate", ThisMonthResCntOfDate);
        items.put("ThisYearResCntOfMonth", ThisYearResCntOfMonth);
        items.put("ThisMonthReqCntOfDate", ThisMonthReqCntOfDate);
        items.put("ThisYearReqCntOfMonth", ThisYearReqCntOfMonth);
        result.data = items;

        return result;
    }
    @GetMapping("/bindSpjangcd")
    public AjaxResult bindSpjangcd(Authentication auth) {
        // 관리자 사용가능 페이지 사업장 코드 선택 로직
        User user = (User) auth.getPrincipal();
        String username = user.getUsername();
        String spjangcd = dashBoardService2.getSpjangcd(username, "");
        // 사업장 코드 선택 로직 종료 반환 spjangcd 활용
        AjaxResult result = new AjaxResult();
        result.data = spjangcd;
        return result;
    }

}
