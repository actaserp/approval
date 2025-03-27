package mes.domain.repository.actasRepository;

import mes.domain.entity.actasEntity.TB_XUSERS;
import mes.domain.entity.actasEntity.TB_XUSERSId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TB_xusersRepository extends JpaRepository<TB_XUSERS, TB_XUSERSId> {

  @Query("SELECT MAX(t.id.cltcd) FROM TB_XCLIENT t")
  String findMaxCltcd();

  void deleteById(String username);

  Optional<TB_XUSERS> findByIdUserid(String userid);
}
