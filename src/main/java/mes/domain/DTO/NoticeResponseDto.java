package mes.domain.DTO;


import mes.domain.entity.sportsEntity.TB_BBSINFO;

import java.io.Serializable;
import java.math.BigInteger;

public record NoticeResponseDto
        (
                Integer id,
                BigInteger num,
                String regDate,
                String title,
                String nickname,
                String content,
                String tel,
                String startDate,
                String endDate,
                String inuserid
        ) implements Serializable {

    private static final long serialVersionUID = 1L;

    // 생성자
    public static NoticeResponseDto from(TB_BBSINFO notice) {
        return new NoticeResponseDto(
                notice.getBBSSEQ(),
                null,
                notice.getBBSDATE(),
                notice.getBBSSUBJECT(),
                notice.getBBSUSER(),
                notice.getBBSTEXT(),
                notice.getBBSTEL(),
                notice.getBBSFRDATE(),
                notice.getBBSTODATE(),
                notice.getINUSERID()
        );
    }
}
