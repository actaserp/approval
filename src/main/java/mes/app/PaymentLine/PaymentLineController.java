package mes.app.PaymentLine;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import mes.app.PaymentLine.Service.PaymentLineService;
import mes.app.PaymentStatus.Service.SportsNoticeService;
import mes.config.Settings;
import mes.domain.entity.User;
import mes.domain.entity.sportsEntity.TB_BBSINFO;
import mes.domain.entity.sportsEntity.TB_FILEINFO;
import mes.domain.model.AjaxResult;
import mes.domain.repository.sportsRepository.BBSINFORepository;
import mes.domain.repository.sportsRepository.FILEINFORepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
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
@RequestMapping("/api/paymentLine")
public class PaymentLineController {
    @Autowired
    PaymentLineService paymentLineService;

    @Autowired
    private Settings settings;

    // 문서코드 리스트
    @GetMapping("/read")
    public AjaxResult read(@RequestParam String comcd
            , Authentication auth){

        User user = (User) auth.getPrincipal();
        String username = user.getUsername();
        String perid = paymentLineService.getPerid(username);
        String splitPerid = perid.replaceFirst("p", ""); // ✅ 첫 번째 "p"만 제거
//        Map<String, Object> userInfo = requestService.getUserInfo(username);

        List<Map<String, Object>> items = this.paymentLineService.getPaymentList(splitPerid, comcd);
        for (Map<String, Object> paperInfo : items){
            String kcperid = "p" + paperInfo.get("perid");
            Map<String, Object> kcInfo = paymentLineService.getuserInfoPerid(kcperid);
            paperInfo.put("pernm", kcInfo.get("pernm"));
        }
        AjaxResult result = new AjaxResult();
        result.data = items;

        return result;
    }
    // 문서코드 리스트
    @GetMapping("/readLine")
    public AjaxResult readLine(@RequestParam String comcd,
                               @RequestParam String perid,
                               Authentication auth){
        User user = (User) auth.getPrincipal();
        String username = user.getUsername();
//        String perid = paymentLineService.getPerid(username);
//        String splitPerid = perid.replaceFirst("p", ""); // ✅ 첫 번째 "p"만 제거
//        Map<String, Object> userInfo = requestService.getUserInfo(username);

        List<Map<String, Object>> items = this.paymentLineService.getCheckPaymentList(perid, comcd);
        for (Map<String, Object> paperInfo : items){
            String kcperid = "p" + paperInfo.get("kcperid");
            Map<String, Object> kcInfo = paymentLineService.getuserInfoPerid(kcperid);
            paperInfo.put("kcpernm", kcInfo.get("pernm"));

            String gubunnm = paymentLineService.getGubuncd((String)paperInfo.get("gubun"));
            paperInfo.put("gubunnm", gubunnm);
        }
        AjaxResult result = new AjaxResult();
        result.data = items;

        return result;
    }
}
