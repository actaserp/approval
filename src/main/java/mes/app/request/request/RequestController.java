package mes.app.request.request;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import mes.app.request.request.service.RequestService;
import mes.config.Settings;
import mes.domain.entity.User;
import mes.domain.entity.actasEntity.*;
import mes.domain.entity.sportsEntity.TB_E063;
import mes.domain.entity.sportsEntity.TB_E063_PK;
import mes.domain.entity.sportsEntity.TB_E064;
import mes.domain.entity.sportsEntity.TB_E064_PK;
import mes.domain.model.AjaxResult;
import mes.domain.repository.actasRepository.TB_DA006WFILERepository;
import mes.domain.repository.actasRepository.TB_DA006WRepository;
import mes.domain.repository.actasRepository.TB_DA007WRepository;
import mes.domain.repository.sportsRepository.E063Repository;
import mes.domain.repository.sportsRepository.E064Repository;
import mes.domain.repository.sportsRepository.E080Repository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.configurationprocessor.json.JSONException;
import org.springframework.boot.configurationprocessor.json.JSONObject;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@RestController
@RequestMapping("/api/request/request")
public class RequestController {
    @Autowired
    private RequestService requestService;

    @Autowired
    private E063Repository e063Repository;

    @Autowired
    private E064Repository e064Repository;

    @Autowired
    private E080Repository e080Repository;

    @Autowired
    Settings settings;
    // 결재라인등록 그리드 read
    @GetMapping("/read")
    public AjaxResult getList(@RequestParam Map<String, String> params
                            , Authentication auth) {
        User user = (User) auth.getPrincipal();
        String username = user.getUsername();
        String perid = requestService.getPerid(username);
        String splitPerid = perid.replaceFirst("p", ""); // ✅ 첫 번째 "p"만 제거
//        Map<String, Object> userInfo = requestService.getUserInfo(username);
        String comcd = params.get("comcd");

        List<Map<String, Object>> items = this.requestService.getCheckPaymentList(splitPerid, comcd);
        for (Map<String, Object> paperInfo : items){
            String kcperid = "p" + paperInfo.get("kcperid");
            Map<String, Object> kcInfo = requestService.getuserInfoPerid(kcperid);
            paperInfo.put("kcpernm", kcInfo.get("pernm"));
        }
        AjaxResult result = new AjaxResult();
        result.data = items;

        return result;
    }

    // 문서코드 옵션 불러오기
    @GetMapping("/getComcd")
    public AjaxResult getListHgrb(){
        List<Map<String, Object>> items = this.requestService.getComcd();

        AjaxResult result = new AjaxResult();
        result.data = items;
        return result;
    }
    // 문서에 따른 결재자 옵션 불러오기
    @GetMapping("/getKcperid")
    public AjaxResult getKcperid(){
        List<Map<String, Object>> items = this.requestService.getKcperid();

        AjaxResult result = new AjaxResult();
        result.data = items;
        return result;
    }

    // 유저정보 불러와 input태그 value
    @GetMapping("/getUserInfo")
    public AjaxResult getUserInfo(Authentication auth){
        User user = (User) auth.getPrincipal();
        String username = user.getUsername();
        String perid = requestService.getPerid(username);
        String splitPerid = perid.replaceFirst("p", ""); // ✅ 첫 번째 "p"만 제거
        Map<String, Object> userInfo = requestService.getMyInfo(username);
        userInfo.put("perid", splitPerid);

        AjaxResult result = new AjaxResult();
        result.data = userInfo;
        return result;
    }

    // 삭제 메서드
    @PostMapping("/delete")
    public AjaxResult deleteHead(@RequestParam String reqnum) {
        AjaxResult result = new AjaxResult();

            boolean success = requestService.delete(reqnum);

            if (success) {
                result.success = true;
                result.message = "삭제하였습니다.";
            } else {
                result.success = false;
                result.message = "삭제에 실패하였습니다.";
            }
        return result;
    }
    // 결재라인 등록
    @PostMapping("/save")
    public AjaxResult saveOrder(@RequestParam Map<String, String> params,
                                Authentication auth) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        AjaxResult result = new AjaxResult();
        TB_E063_PK headpk = new TB_E063_PK();
        TB_E063 head = new TB_E063();
        TB_E064_PK bodypk = new TB_E064_PK();
        TB_E064 body = new TB_E064();
        LocalDateTime createdAt = LocalDateTime.now();
        String indate = createdAt.format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        User user = (User)auth.getPrincipal();
        String username = user.getUsername();
        Map<String, Object> userInfo = requestService.getMyInfo(username);

        // 063 테이블 선언
        headpk.setCustcd((String) userInfo.get("custcd"));
        headpk.setPerid(params.get("perid"));
        headpk.setPapercd(params.get("papercd"));
        headpk.setSpjangcd((String) userInfo.get("spjangcd"));

        head.setId(headpk);
        head.setInperid(username);
        head.setIndate(indate);



        // 064테이블 선언
        bodypk.setCustcd((String) userInfo.get("custcd"));
        bodypk.setPerid(params.get("perid"));
        bodypk.setSpjangcd((String) userInfo.get("spjangcd"));
        bodypk.setNo(params.get("seq"));
        bodypk.setPapercd(params.get("papercd"));

        body.setId(bodypk);
        body.setInperid(username);
        body.setGubun(params.get("gubun"));
        body.setKcchk("1");
        body.setKcperid(params.get("kcperid"));
        body.setSeq(params.get("seq"));
        body.setIndate(indate);
        // 데이터 insert
        try {
            e063Repository.save(head);
            e064Repository.save(body);

            result.success = true;
            result.message = "결재라인정보 저장 성공";
        }catch (Exception e) {
            result.success = false;
            result.message = "결재라인정보 저장 실패(" + e.getMessage() + ")";
        }
        return result;
    }

}

