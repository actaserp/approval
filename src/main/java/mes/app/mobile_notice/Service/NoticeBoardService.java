package mes.app.mobile_notice.Service;


import mes.domain.DTO.FileResponseDto;
import mes.domain.DTO.NoticeResponseDto;
import mes.domain.repository.sportsRepository.BBSINFORepository;
import mes.domain.repository.sportsRepository.FILEINFORepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityNotFoundException;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class NoticeBoardService {

    @Autowired
    BBSINFORepository bbsinfoRepository;

    @Autowired
    FILEINFORepository fileinfoRepository;


    @Transactional(readOnly = true)
    public Page<NoticeResponseDto> NoticeList(Pageable pageable, String searchKeyword){

        int page = pageable.getPageNumber();
        int size = pageable.getPageSize();
        int offset = page * size;
        List<Map<String, Object>> results = bbsinfoRepository.findAllWithPagination(offset, size, searchKeyword);

        List<NoticeResponseDto> noticeList =
                results.stream()
                        .map(row -> new NoticeResponseDto(
                                (Integer) row.get("BBSSEQ"),
                                (BigInteger) row.get("num"),
                                (String) row.get("BBSDATE"),
                                (String) row.get("BBSSUBJECT"),
                                (String) row.get("BBSUSER"),
                                (String) row.get("BBSTEXT"),
                                (String) row.get("BBSTEL"),
                                (String) row.get("BBSFRDATE"),
                                (String) row.get("BBSTODATE"),
                                (String) row.get("INUSERID")
                        )).collect(Collectors.toList());

        long totalCount = bbsinfoRepository.countWithPagination(searchKeyword);

        return new PageImpl<>(noticeList, pageable, totalCount);

    }

    @Transactional(readOnly = true)
    public NoticeResponseDto NoticeView(Integer id){

        NoticeResponseDto byBBSSEQ = bbsinfoRepository.findByBBSSEQ(id).map(NoticeResponseDto::from).orElseThrow(() ->
                new EntityNotFoundException("조회에 필요한 정보를 못찾음"));

        return byBBSSEQ;
    }

    @Transactional(readOnly = true)
    public List<FileResponseDto> NoticeFile(Integer id){

        List<FileResponseDto> byBBSSEQ = fileinfoRepository.findByBbsseq(id).stream()
                .map(row -> new FileResponseDto(
                        row.getFileseq(),
                        row.getFiledate(),
                        row.getCHECKSEQ(),
                        row.getBbsseq(),
                        row.getFILEPATH(),
                        row.getFILESVNM(),
                        row.getFILEEXTNS(),
                        row.getFILEORNM()

                )).toList();

        return byBBSSEQ;
    }


}
