package mes.domain.repository.sportsRepository;

import mes.domain.entity.sportsEntity.TB_E063;
import mes.domain.entity.sportsEntity.TB_E064;
import mes.domain.entity.sportsEntity.TB_E064_PK;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface E064Repository extends JpaRepository<TB_E064, TB_E064_PK> {


}
