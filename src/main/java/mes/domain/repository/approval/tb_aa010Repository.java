package mes.domain.repository.approval;

import mes.domain.entity.approval.TB_AA010PDF;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface tb_aa010Repository extends JpaRepository<TB_AA010PDF, String> {

  boolean existsBySpdateAndFilenameIsNotNull(String appnum);

  TB_AA010PDF findBySpdate(String appnum);
}
