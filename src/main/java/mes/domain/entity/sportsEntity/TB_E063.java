package mes.domain.entity.sportsEntity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;

@Entity
@Table(name = "TB_E063")
@Setter
@Getter
@NoArgsConstructor
public class TB_E063 {

    @EmbeddedId
    private TB_E063_PK id;

    @Column(length = 100)
    private String remark;

    @Column(length = 10)
    private String inperid;

    @Column(length = 8, updatable = false)
    private String indate;
}
