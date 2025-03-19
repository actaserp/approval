package mes.app.system;

import java.sql.Types;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import mes.app.system.service.UserCodeService;
import mes.domain.entity.User;
import mes.domain.entity.UserCode;
import mes.domain.model.AjaxResult;
import mes.domain.repository.UserCodeRepository;


@RestController
@RequestMapping("/api/system/code")
public class UserCodeController {

    @Autowired
    private UserCodeService codeService;

    @Autowired
    UserCodeRepository userCodeRepository;

    @Autowired
    JdbcTemplate jdbcTemplate;


    @GetMapping("/read")
    public AjaxResult getCodeList(
            @RequestParam("txtCode") String txtCode
    ) {

        List<Map<String, Object>> items = this.codeService.getCodeList(txtCode);
        AjaxResult result = new AjaxResult();

        result.data = items;
        return result;
    }

    @GetMapping("/detail")
    public AjaxResult getCode(@RequestParam("id") int id) {
        Map<String, Object> item = this.codeService.getCode(id);

        AjaxResult result = new AjaxResult();
        result.data = item;
        return result;
    }

    @PostMapping("/save")
    public AjaxResult saveCode(
        @RequestParam(value = "id", required = false) Integer id,
        @RequestParam("name") String value,
        @RequestParam("code") String code,
        @RequestParam(value = "parent_id", required = false) Integer parent_id,
        @RequestParam("description") String description,
        @RequestParam("status") String status,
        HttpServletRequest request,
        Authentication auth) {

        /*log.info("Received Data - id: {}, code: {}, name: {}, parent_id: {}, description: {}, status: {}",
                id, code, value, parent_id, description, status);*/

        User user = (User) auth.getPrincipal();

        UserCode c = null;

        if (id == null) {
            c = new UserCode();
        } else {
            c = this.userCodeRepository.getUserCodeById(id);
        }

        c.setValue(value);
        c.setCode(code);
        c.setDescription(description);
        c.setParentId(parent_id);
        c.set_audit(user);
        c.set_status(status);

        c = this.userCodeRepository.save(c);

        AjaxResult result = new AjaxResult();
        result.data = c;

        return result;
    }

    @PostMapping("/delete")
    @Transactional
    public AjaxResult deleteCode(@RequestParam("id") Integer id) {
        this.userCodeRepository.deleteById(id);
        AjaxResult result = new AjaxResult();

        return result;
    }

/*    @GetMapping("/checkDuplicate")
    public ResponseEntity<Map<String, Boolean>> checkDuplicate(@RequestParam String code) {
        boolean exists = codeService.existsByCode(code);
        Map<String, Boolean> response = new HashMap<>();
        response.put("exists", exists);
        return ResponseEntity.ok(response);
    }*/

    @GetMapping("/checkDuplicate")
    public ResponseEntity<Map<String, Boolean>> checkDuplicate(@RequestParam String code) {
        boolean exists = codeService.existsByCode(code);
        Map<String, Boolean> response = new HashMap<>();
        response.put("exists", exists);
        return ResponseEntity.ok(response);
    }

}