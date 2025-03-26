package mes.app.mobile_production;

import mes.app.mobile_production.service.ProductionService;
import mes.domain.entity.User;
import mes.domain.entity.actasEntity.TB_DA006W_PK;
import mes.domain.model.AjaxResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.text.NumberFormat;
import java.util.*;

@RestController
@RequestMapping("/api/mobile_production")
public class ProductionController {
    @Autowired
    private ProductionService productionService;
    // 프로시저 리스트
    @GetMapping("/read_all")
    public AjaxResult productionList(@RequestParam(value = "search_startDate", required = false) String searchStartDate,
                                     @RequestParam(value = "search_endDate", required = false) String searchEndDate,
                                     @RequestParam(value = "search_subject", required = false) String searchSubject,
                                     @RequestParam(value = "search_gubun", required = false) String searchGubun,
                                     Authentication auth) {
        User user = (User) auth.getPrincipal();
        String username = user.getUsername();
        Map<String, Object> userInfo = productionService.getUserInfo(username);
        String search_startDate = (searchStartDate).replaceAll("-","");
        String search_endDate = (searchEndDate).replaceAll("-","");

        Map<String, Object> searchLabels = new HashMap<>();
        searchLabels.put("search_spjangcd", userInfo.get("spjangcd"));
        String perid = userInfo.get("perid").toString();
        String splitPerid = perid.replaceFirst("p", ""); // ✅ 첫 번째 "p"만 제거
        searchLabels.put("search_perid", splitPerid);
        searchLabels.put("search_startDate", search_startDate);
        searchLabels.put("search_endDate", search_endDate);
        searchLabels.put("search_subject", searchSubject);
        searchLabels.put("search_gubun", searchGubun);
        List<Map<String, Object>> totalList = productionService.getProductionList(searchLabels);
        List<Map<String, Object>> vacList = productionService.getVacList(searchLabels);
        List<Map<String, Object>> allList = new ArrayList<>();
        allList.addAll(totalList);
        allList.addAll(vacList);
        allList.sort(Comparator.comparing(
                m -> (String) m.get("spdate"),
                Comparator.nullsLast(Comparator.reverseOrder())
        ));
        for(Map<String, Object> item : allList) {
            // 날짜 형식 변환 (spdate)
            if (item.containsKey("spdate")) {
                String setupdt = (String) item.get("spdate");
                if (setupdt != null && setupdt.length() == 8) {
                    String formattedDate = setupdt.substring(0, 4) + "-" + setupdt.substring(4, 6) + "-" + setupdt.substring(6, 8);
                    item.put("spdate", formattedDate);
                }
            }
            // title 형식변환 (e080_title)
            if (item.containsKey("e080_title")) {
                String setup = (String) item.get("e080_title");
                if (setup == null || setup.isEmpty()) {
                    item.put("e080_title", '-');
                }
            }
            // remark 형식변환 (remark)
            if (item.containsKey("remark")) {
                String setup = (String) item.get("remark");
                if (setup == null || setup.isEmpty()) {
                    item.put("remark", '-');
                }
            }
            // 금액 비교 (dramt, cramt)
            if (item.containsKey("dramt")) {
                Number dramt = (Number) item.getOrDefault("dramt", 0);
                Number cramt = (Number) item.getOrDefault("cramt", 0);
                int setamt = Math.max(dramt.intValue(), cramt.intValue());
                item.put("setamt", setamt);

                // 쉼표 포함 포맷팅
                NumberFormat formatter = NumberFormat.getNumberInstance(Locale.KOREA);
                String setamtStr = formatter.format(setamt);
                item.put("setamtStr", setamtStr);
            }
            // 휴가신청서 구분 포맷팅 (appperid -> apppernm)
            if (item.containsKey("yearflag")) {
                if(item.get("yearflag").toString().equals("0")){
                    item.put("yearflag", "근태");
                }
                if(item.get("yearflag").toString().equals("1")){
                    item.put("yearflag", "연차차감");
                }
                if(item.get("yearflag").toString().equals("2")){
                    item.put("yearflag", "대체휴가생성");
                }
            }
            // 휴가신청서 휴가일 포맷팅 (vafrdate)
            if (item.containsKey("vafrdate")) {
                String setupdt = (String) item.get("vafrdate");
                if (setupdt != null && setupdt.length() == 8) {
                    String formattedDate = setupdt.substring(0, 4) + "." + setupdt.substring(4, 6) + "." + setupdt.substring(6, 8);
                    item.put("vafrdate", formattedDate);
                }
            }
            // 휴가신청서 휴가일 포맷팅 (vatodate)
            if (item.containsKey("vatodate")) {
                String setupdt = (String) item.get("vatodate");
                if (setupdt != null && setupdt.length() == 8) {
                    String formattedDate = setupdt.substring(0, 4) + "." + setupdt.substring(4, 6) + "." + setupdt.substring(6, 8);
                    item.put("vatodate", formattedDate);
                }
            }
            // 휴가신청서 휴가시간 포맷팅 (vafrtime)
            if (item.containsKey("vafrtime")) {
                String setupdt = (String) item.get("vafrtime");
                if (setupdt != null && setupdt.length() == 4) {
                    String formattedTime = setupdt.substring(0, 2) + ":" + setupdt.substring(2, 4);
                    item.put("vafrtime", formattedTime);
                }else{
                    item.put("vafrtime", "-");
                }
            }
            // 휴가신청서 휴가일 포맷팅 (vatotime)
            if (item.containsKey("vatotime")) {
                String setupdt = (String) item.get("vatotime");
                if (setupdt != null && setupdt.length() == 4) {
                    String formattedTime = setupdt.substring(0, 2) + ":" + setupdt.substring(2, 4);
                    item.put("vatotime", formattedTime);
                }else{
                    item.put("vatotime", "-");
                }
            }
        }
        AjaxResult result = new AjaxResult();

        System.out.println("vacList : " + vacList);
        result.data = allList;
        return result;
    }
    // 작업이력 팝업 데이터
    @GetMapping("/read_work")
    public AjaxResult workList(@RequestParam(value = "wono", required = false) String wono,
                                     Authentication auth) {
        User user = (User) auth.getPrincipal();
        String username = user.getUsername();
        Map<String, Object> userInfo = productionService.getUserInfo(username);

        Map<String, Object> searchLabels = new HashMap<>();
        searchLabels.put("search_spjangcd", (String) userInfo.get("spjangcd"));
        searchLabels.put("search_custcd", (String) userInfo.get("custcd"));
        searchLabels.put("wono", wono);
        List<Map<String, Object>> productList = productionService.getWorkList(searchLabels);
        productList.forEach(product -> {
            // 기존 값을 가져오기
            if(product.get("wflag") != null) {
                String originalValue1 = product.get("wflag").toString();
                if (originalValue1.length() > 2) {
                    originalValue1 = originalValue1.substring(2);
                    // 새로운 값으로 업데이트
                    Map<String, Object> newValue = productionService.getProcess(originalValue1);
                    // 맵에 업데이트된 값 넣기
                    product.put("wflag", newValue.get("com_cnam"));
                }
            }

            // 날짜 형식 변환 (wtrdt)
            if (product.containsKey("wtrdt")) {
                String setupdt = (String) product.get("wtrdt");
                if (setupdt != null && setupdt.length() == 8) {
                    String formattedDate = setupdt.substring(0, 4) + "-" + setupdt.substring(4, 6) + "-" + setupdt.substring(6, 8);
                    product.put("wtrdt", formattedDate);
                }
            }
        });
        AjaxResult result = new AjaxResult();
        result.data = productList;
        return result;
    }
    // 거래처 검색
    @GetMapping("/search_cltcd")
    public AjaxResult searchCltcd(@RequestParam(value = "search_cltnm", required = false) String search_cltnm,
                               Authentication auth) {

        List<Map<String, Object>> productList = productionService.searchCltcd(search_cltnm);

        AjaxResult result = new AjaxResult();
        result.data = productList;
        return result;
    }
    // 품목 검색
    @GetMapping("/search_product")
    public AjaxResult searchProduct(@RequestParam(value = "search_productnm", required = false) String search_product,
                               Authentication auth) {

        List<Map<String, Object>> productList = productionService.searchProduct(search_product);

        AjaxResult result = new AjaxResult();
        result.data = productList;
        return result;
    }
    // 그리드 리스트
    @GetMapping("/todayGrid")
    public AjaxResult searchTodayGrid(@RequestParam(value = "search_startDate", required = false) String searchStartDate,
                                      @RequestParam(value = "search_endDate", required = false) String searchEndDate,
                                      @RequestParam(value = "search_cltcd", required = false) String searchCltcd,
                                      @RequestParam(value = "search_product", required = false) String searchPcode,
                                      Authentication auth) {
        User user = (User) auth.getPrincipal();
        String username = user.getUsername();
        Map<String, Object> userInfo = productionService.getUserInfo(username);
        String search_startDate = (searchStartDate).replaceAll("-","");

        Map<String, Object> searchLabels = new HashMap<>();
        searchLabels.put("search_spjangcd", (String) userInfo.get("spjangcd"));
        searchLabels.put("search_custcd", (String) userInfo.get("custcd"));
        searchLabels.put("search_startDate", search_startDate);
        searchLabels.put("search_cltcd", searchCltcd);
        searchLabels.put("search_pcode", searchPcode);

        List<Map<String, Object>> productList = productionService.searchTodayGrid(searchLabels);

        AjaxResult result = new AjaxResult();
        result.data = productList;
        return result;
    }
}
