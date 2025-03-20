package mes.domain.entity.sportsEntity;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Embeddable;

@Embeddable
@Getter
@Setter
@EqualsAndHashCode
public class TB_E064_PK implements Serializable {

    @Column(length = 8, nullable = false)
    private String custcd;

    @Column(length = 2, nullable = false)
    private String spjangcd;

    @Column(length = 10, nullable = false)
    private String perid;

    @Column(length = 10, nullable = false)
    private String papercd;

    @Column(length = 3, nullable = false)
    private String no;
}
