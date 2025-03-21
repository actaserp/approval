package mes.domain.repository.sportsRepository;

import mes.domain.entity.sportsEntity.TB_E063;
import mes.domain.entity.sportsEntity.TB_E064;
import mes.domain.entity.sportsEntity.TB_E064_PK;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface E064Repository extends JpaRepository<TB_E064, TB_E064_PK> {
    @Query("SELECT MAX(e.id.no) FROM TB_E064 e WHERE e.id.custcd = :custcd AND e.id.spjangcd = :spjangcd AND e.id.perid = :perid AND e.id.papercd = :papercd")
    String findMaxNo(@Param("custcd") String custcd,
                     @Param("spjangcd") String spjangcd,
                     @Param("perid") String perid,
                     @Param("papercd") String papercd);

}
