package mes.app;

import mes.app.mobile_notice.Service.NoticeBoardService;
import mes.domain.DTO.FileResponseDto;
import mes.domain.DTO.NoticeResponseDto;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

// 모바일 메뉴 컨트롤러
@Controller
@RequestMapping("/mobile")
public class MobileController {

    private final NoticeBoardService noticeBoardService;


    public MobileController(NoticeBoardService noticeBoardService) {
        this.noticeBoardService = noticeBoardService;
    }

    @GetMapping("/ticket-list")
    public String ticketList(Model model) {
        model.addAttribute("currentPage", "ticket-list");
        return "mobile/ticket-list"; // "mobile/ticket-list.html"로 매핑
    }

    @GetMapping("/work-list")
    public String workList(Model model) {
        model.addAttribute("currentPage", "work-list");
        return "mobile/work-list"; // 작지별 현황
    }
    @GetMapping("/current-status")
    public String currentStatusPage(Model model) {
        model.addAttribute("currentPage", "current-status");
        return "mobile/current-status"; // Return the Thymeleaf view name
    }

    @GetMapping("/notice/view")
    public String NoticePage(Model modelMap, @RequestParam Integer id) {


        NoticeResponseDto detail = noticeBoardService.NoticeView(id);

        List<FileResponseDto> file = noticeBoardService.NoticeFile(id);

        if(!file.isEmpty()){
            modelMap.addAttribute("fileflag", true);
        }else{
            modelMap.addAttribute("fileflag", false);
        }
        modelMap.addAttribute("currentPage", "notice");
        modelMap.addAttribute("notice", detail);

        return "mobile/noticeView";
    }

    @GetMapping("/fsr-register")
    public String fsrRegister() {
        return "mobile/fsr-register"; // "mobile/fsr-register.html"로 매핑
    }

    @GetMapping("/fsr-search")
    public String fsrSearch() {
        return "mobile/fsr-search"; // "mobile/fsr-search.html"로 매핑
    }

    @GetMapping("/user-info")
    public String userInfo() {
        return "mobile/user-info"; // "mobile/user-info.html"로 매핑
    }
}