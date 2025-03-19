package mes.app.alarm;


import mes.app.alarm.Service.AlarmService;
import mes.domain.entity.User;
import mes.domain.entity.UserGroup;
import mes.domain.model.AjaxResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/Alarm")
public class AlarmController {

    @Autowired
    AlarmService alarmService;

    @GetMapping("/notifications")
    public AjaxResult getNotifications(Authentication auth) {
        AjaxResult result = new AjaxResult();
        try {
            // 로그인한 사용자 정보
            User user = (User) auth.getPrincipal();
            String userId = user.getUsername();
            String userCd = alarmService.getPerid(userId);
            String userCode = userCd.replaceFirst("p", ""); // ✅ 첫 번째 "p"만 제거

            String spjangcd = user.getSpjangcd();
            int userGroupId = user.getUserProfile().getUserGroup().getId();

            // 알림 데이터 조회
            List<Map<String, Object>> notifications = alarmService.getNotifications(userCode, spjangcd, userGroupId);

            result.success = true;
            result.data = notifications;
            result.message = "알림 조회 성공";
        } catch (Exception e) {
            result.success = false;
            result.message = "알림 데이터를 가져오는 중 오류가 발생했습니다.";
        }

        return result;
    }

    @PostMapping("markAsRead")
    public AjaxResult markNotificationsAsRead(Authentication auth) {
        AjaxResult result = new AjaxResult();

        try {
            // 로그인한 사용자 정보 가져오기
            User user = (User) auth.getPrincipal();
            String username = user.getUsername();
            String userCd = alarmService.getPerid(username);
            String userCode = userCd.replaceFirst("p", ""); // ✅ 첫 번째 "p"만 제거
            String spjangcd = user.getSpjangcd();
            UserGroup userGroupId = user.getUserProfile().getUserGroup();

            // 알림 상태 업데이트
            alarmService.markAsRead(userCode, spjangcd, userGroupId);

            result.success = true;
            result.message = "알림 상태가 업데이트되었습니다.";
        } catch (Exception e) {
            result.success = false;
            result.message = "알림 상태 업데이트 중 오류가 발생했습니다.";
            e.printStackTrace();
        }

        return result;
    }

}
