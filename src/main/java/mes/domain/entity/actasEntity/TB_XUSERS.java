package mes.domain.entity.actasEntity;


import lombok.*;

import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name = "TB_XUSERS")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TB_XUSERS {

    @EmbeddedId
    private TB_XUSERSId id;

    @Column(name = "rnum")
    private String rnum;

    @Column(name = "passwd1")   //비밀번호
    private String passwd1;

    @Column(name = "passwd2")   //비밀번호
    private String passwd2;

    @Column(name = "custnm")
    private String custnm;

    @Column(name = "pernm")     //이름
    private String pernm;

    @Column(name = "useyn")     //사용여부?
    private String useyn;

    @Column(name = "perid")
    private String perid;   //사원코드

    @Column(name = "sysmain")
    private String sysmain;

    @Column(name = "grpid")
    private String grpid;

    @Column(name = "spjangcd")
    private String spjangcd;
}

