package mes.app.request.request;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import mes.app.request.request.service.RequestService;
import mes.config.Settings;
import mes.domain.entity.User;
import mes.domain.entity.actasEntity.*;
import mes.domain.model.AjaxResult;
import mes.domain.repository.actasRepository.TB_DA006WFILERepository;
import mes.domain.repository.actasRepository.TB_DA006WRepository;
import mes.domain.repository.actasRepository.TB_DA007WRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.configurationprocessor.json.JSONException;
import org.springframework.boot.configurationprocessor.json.JSONObject;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.transaction.Transactional;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@RestController
@RequestMapping("/api/request/request")
public class RequestController {
    @Autowired
    private RequestService requestService;

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

    // 유저정보 불러와 input태그 value
    @GetMapping("/getUserInfo")
    public AjaxResult getUserInfo(Authentication auth){
        User user = (User) auth.getPrincipal();
        String username = user.getUsername();
        Map<String, Object> userInfo = requestService.getMyInfo(username);

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

}

