package mes.domain.entity.actasEntity;

import groovy.transform.builder.Builder;
import lombok.*;

import javax.persistence.Entity;
import javax.persistence.Table;

import javax.persistence.*;

@Builder
@Entity
@Table(name = "TB_XA012")
@Setter
@Getter
@NoArgsConstructor
public class TB_XA012 {

    @EmbeddedId
    private TB_XA012ID id;

    @Column(name = "saupnum", length = 13)
    private String saupnum; // 사업자 번호

    @Column(name = "spjangnm", length = 40)
    private String spjangnm;

    @Column(name = "compnm", length = 40)
    private String compnm; // 업체명

    @Column(name = "prenm", length = 30)
    private String prenm; // 대표자

    @Column(name = "openymd", length = 8)
    private String openymd;

    @Column(name = "custperclsf", length = 1)
    private String custperclsf;

    @Column(name = "corpnum", length = 13)
    private String corpnum;

    @Column(name = "jointsaup", length = 60)
    private String jointsaup;

    @Column(name = "zipcd", length = 6)
    private String zipcd; // 우편번호

    @Column(name = "adresa", length = 60)
    private String adresa; // 주소1

    @Column(name = "adresb", length = 60)
    private String adresb; // 주소2

    @Column(name = "zipcd2", length = 6)
    private String zipcd2; // 추가 우편번호

    @Column(name = "adres2a", length = 60)
    private String adres2a; // 추가 주소1

    @Column(name = "adres2b", length = 60)
    private String adres2b; // 추가 주소2

    @Column(name = "biztype", length = 40)
    private String biztype;

    @Column(name = "item", length = 40)
    private String item;

    @Column(name = "tel1", length = 30)
    private String tel1; // 전화번호1

    @Column(name = "tel2", length = 30)
    private String tel2; // 전화번호2

    @Column(name = "fax", length = 30)
    private String fax; // 팩스 번호

    @Column(name = "buzclaif", length = 2)
    private String buzclaif;

    @Column(name = "comtaxoff", length = 3)
    private String comtaxoff;

    @Column(name = "emailadres", length = 30)
    private String emailadres; // 이메일 주소

    @Column(name = "passwd", length = 4)
    private String passwd; // 비밀번호

    @Column(name = "astvaluemet", length = 2)
    private String astvaluemet;

    @Column(name = "agnertel1", length = 30)
    private String agnertel1;

    @Column(name = "agnertel2", length = 30)
    private String agnertel2;

    @Column(name = "stdate", length = 8)
    private String stdate;

    @Column(name = "eddate", length = 8)
    private String eddate;

    @Column(name = "operdivsn", length = 1)
    private String operdivsn;

    @Column(name = "tel3", length = 30)
    private String tel3; // 전화번호3

    @Column(name = "allpay", length = 1)
    private String allpay;

    @Column(name = "halfpay", length = 1)
    private String halfpay;

    @Column(name = "taxagentcd", length = 6)
    private String taxagentcd;

    @Column(name = "taxagentnm", length = 30)
    private String taxagentnm;

    @Column(name = "ctano", length = 10)
    private String ctano;

    @Column(name = "taxagentsp", length = 10)
    private String taxagentsp;

    @Column(name = "taxagenttel", length = 18)
    private String taxagenttel;

    @Column(name = "guchung", length = 40)
    private String guchung;

//    @Column(name = "espjangnm", length = 40)
//    private String espjangnm;
//
//    @Column(name = "ezipcd", length = 20)
//    private String ezipcd;

/*    @Column(name = "eadresa", length = 100)
    private String eadresa;
    @Column(name = "eadresb", length = 100)
    private String eadresb;


    @Column(name = "etelno", length = 30)
    private String etelno;

    @Column(name = "efaxno", length = 30)
    private String efaxno;

    @Column(name = "epernm", length = 30)
    private String epernm;*/


}
