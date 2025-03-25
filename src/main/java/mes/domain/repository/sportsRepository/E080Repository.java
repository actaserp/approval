package mes.domain.repository.sportsRepository;

import mes.domain.entity.sportsEntity.TB_E063;
import mes.domain.entity.sportsEntity.TB_E080;
import mes.domain.entity.sportsEntity.TB_E080_PK;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface E080Repository extends JpaRepository<TB_E080, TB_E080_PK> {


}
