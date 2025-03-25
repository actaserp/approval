package mes.domain.repository.approval;

import mes.domain.entity.approval.TB_AA009;
import mes.domain.entity.approval.TB_AA009_PK;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TB_AA009Repository extends JpaRepository<TB_AA009, TB_AA009_PK> {
}
