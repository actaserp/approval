package mes.domain.repository.approval;

import mes.domain.entity.approval.TB_AA010ATCH;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TB_AA010ATCHRepository extends JpaRepository<TB_AA010ATCH, String> {
  boolean existsBySpdateAndFilenameIsNotNull(String appnum);

  TB_AA010ATCH findBySpdate(String appnum);
}
