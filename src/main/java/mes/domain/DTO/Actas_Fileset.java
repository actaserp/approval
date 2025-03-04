package mes.domain.DTO;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Actas_Fileset {

    private String fileNm;



    private Long fileSize;
    private String fileExt;
    private Boolean success;


}
