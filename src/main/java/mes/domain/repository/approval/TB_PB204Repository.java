package mes.domain.repository.approval;

import mes.domain.entity.approval.TB_PB204;
import mes.domain.entity.approval.TB_PB204_PK;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TB_PB204Repository extends JpaRepository<TB_PB204, TB_PB204_PK> {

  boolean existsByAppnum(String appnum);

}
