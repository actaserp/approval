package mes.app.account.service;

import lombok.extern.slf4j.Slf4j;
import mes.domain.entity.actasEntity.TB_XUSERS;
import mes.domain.entity.actasEntity.TB_XUSERSId;
import mes.domain.repository.actasRepository.TB_xuserstRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
public class TB_xusersService {
    @Autowired
    TB_xuserstRepository xuserstRepository;

    public void save(TB_XUSERS xusers) {
        xuserstRepository.save(xusers);
    }

    public Optional<TB_XUSERS> findById(TB_XUSERSId tbXusersId) {
        return xuserstRepository.findById(tbXusersId);
    }

}
