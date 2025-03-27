package mes.domain.DTO;


import mes.domain.entity.sportsEntity.TB_FILEINFO;

import java.io.Serializable;

public record FileResponseDto
        (
                int seq,
                String regDate,
                String checkseq,
                int articleNum,
                String path,
                String saveNm,
                String extension,
                String originalName
        ) implements Serializable {
        private static final long serialVersionUID = 1L;
        public static FileResponseDto from(TB_FILEINFO entity){
                return new FileResponseDto(
                        entity.getFileseq(),
                        entity.getFiledate(),
                        entity.getCHECKSEQ(),
                        entity.getBbsseq(),
                        entity.getFILEPATH(),
                        entity.getFILESVNM(),
                        entity.getFILEEXTNS(),
                        entity.getFILEORNM()
                );
        }

}
