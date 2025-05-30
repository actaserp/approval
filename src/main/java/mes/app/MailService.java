package mes.app;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class MailService {


    @Autowired
    private JavaMailSender mailSender;

    //비밀번호 변경 인증
    public void sendVerificationEmail(String to, String usernm, String uuid){
        String subject = "비밀번호 변경 인증 메일입니다.";
        String text = "안녕하세요, " + usernm + "님.\n\n"
                + "다음 인증 코드를 입력하여 비밀번호 재설정을 완료하세요:\n"
                + uuid + "\n\n"
                + "이 코드는 3분 동안 유효합니다.";

        SimpleMailMessage message = new SimpleMailMessage();

        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);
        message.setFrom("replusshare@naver.com");

        mailSender.send(message);
    }

    //사용자등록 (이메일 인증)
    public void saveVerificationEmail(String to, String name, String uuid){
        if (name == null || name.isEmpty()) {
            name = "사용자";
        }
        String subject = "사용자등록 인증 메일입니다.";
        String text = "안녕하세요, " + name + "님.\n\n"
                + "다음 인증 코드를 입력하여 사용자등록을 완료하세요:\n"
                + uuid + "\n\n"
                + "이 코드는 3분 동안 유효합니다.";

        SimpleMailMessage message = new SimpleMailMessage();

        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);
        message.setFrom("replusshare@naver.com");

        mailSender.send(message);
    }

}
