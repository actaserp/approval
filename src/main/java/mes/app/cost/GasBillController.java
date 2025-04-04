package mes.app.cost;


import mes.domain.entity.User;
import mes.domain.entity.actasEntity.TB_RP410;
import mes.domain.repository.actasRepository.TB_RP410Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import mes.app.cost.service.GasBillService;
import mes.app.cost.service.PdfProcessingService;
import mes.domain.model.AjaxResult;
import mes.domain.services.SqlRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.transaction.Transactional;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/cost/gas_bill")
public class GasBillController {

    @Autowired
    SqlRunner sqlRunner;

    @Autowired
    GasBillService gasBillService;

    @Autowired
    private PdfProcessingService pdfProcessingService;

    @Autowired
    TB_RP410Repository tbRp410Repository;

    private static final Logger logger = LoggerFactory.getLogger(GasBillController.class);


    // 특정 연도에 대한 1월부터 12월까지의 데이터를 가져오는 API
    @GetMapping("/read")
    public AjaxResult getGasBillList(@RequestParam("year") String year) {

        AjaxResult result = new AjaxResult();

        List<Map<String, Object>> monthlyData = gasBillService.getMonthlyUsageSummary(year);
        result.data = monthlyData;
        return result;

    }

    // 연도 리스트를 가져오는 API
    @GetMapping("/Select_years")
    public List<String> getAvailableYears() {
        return gasBillService.getYear();
    }


    @GetMapping("/mainread")
    public AjaxResult mainReadList(@RequestParam(value = "searchfrdate", required = false) String searchfrdate,
                                   @RequestParam(value = "searchtodate", required = false) String searchtodate,
                                   @RequestParam(value = "searchYear", required = false) String searchYear,
                                   @RequestParam(value = "searchfrYear", required = false) String searchfrYear,
                                   @RequestParam(value = "searchtoYear", required = false) String searchtoYear,
                                   @RequestParam(value = "searchfrMonth", required = false) String searchfrMonth,
                                   @RequestParam(value = "searchtoMonth", required = false) String searchtoMonth,
                                   @RequestParam(value = "searchType", required = true) String searchType) {

        List<Map<String, Object>> items = new ArrayList<>();
        String startdate = null;
        String endDate = null;

        switch (searchType) {
            case "hourly":
                startdate = searchfrdate.replaceAll("-", "");
                // 시간 관련 조회 로직 추가 가능
                break;
            case "monthly":
                startdate = searchfrdate.replaceAll("-", "");
                endDate = searchtodate.replaceAll("-", "");
                break;
            case "yearly":
                startdate = searchfrYear + "0101";
                endDate = searchtoYear + "1231";
                break;
            // 필요한 경우 다른 타입의 조건 추가
            default:
                throw new IllegalArgumentException("Invalid search type: " + searchType);
        }

        // 서비스에서 데이터 가져오기
        List<Map<String, Object>> mainDataList = gasBillService.getmainData(startdate, endDate, searchType);

        AjaxResult result = new AjaxResult();
        result.data = mainDataList;

        return result;
    }


    // pdf 파일 업로드 및 db 저장
    @PostMapping("/upload")
    @Transactional
    public AjaxResult uploadFile(@RequestParam("file") MultipartFile file,
                                 @RequestParam("spworkcd") String spworkcd,
                                 @RequestParam("spcompcd") String spcompcd,
                                 @RequestParam("spplannm") String spplannm,
                                 @RequestParam("spworknm") String spworknm,
                                 @RequestParam("spcompnm") String spcompnm,
                                 @RequestParam("spplancd") String spplancd,
                                 @RequestParam("standdt") String standdt,
                                 Authentication auth) {
        AjaxResult result = new AjaxResult();

        User user = (User) auth.getPrincipal();
        TB_RP410 rp410 = new TB_RP410();
        File tempFile = null;
        Timestamp now = new Timestamp(System.currentTimeMillis());

        // 파일 유효성 검사
        if (file.isEmpty() || !StringUtils.getFilenameExtension(file.getOriginalFilename()).equalsIgnoreCase("pdf")) {
            logger.warn("파일 업로드 실패: 비어 있거나 PDF 파일이 아닙니다. 파일 이름: {}", file.getOriginalFilename());
            result.success = false;
            result.message = "PDF 파일만 업로드 가능합니다";
            return result;
        }

        try {
            // 업로드된 파일을 임시 위치에 저장
            tempFile = File.createTempFile("uploaded-", ".pdf");
            file.transferTo(tempFile);
//            logger.info("파일 업로드 완료, 임시 파일 경로: {}", tempFile.getAbsolutePath());

            // PDF 파일을 처리하고 텍스트를 추출
            List<String> extractedTexts = pdfProcessingService.processPdf(tempFile.getAbsolutePath());
//            logger.info("PDF 파일 처리 완료, 추출된 텍스트 개수: {}", extractedTexts.size());

            // OCR 응답 파싱 및 데이터 저장
            boolean isStandymSet = false;
            for (String text : extractedTexts) {
                String parsedText = pdfProcessingService.parseOcrResponse(text);

                // standym 값이 포함된 텍스트를 찾고 변환
                if (!isStandymSet && parsedText.contains("년") && parsedText.contains("월")) {
                    String formattedStandym = convertToStandymFormat(parsedText);
                    if (formattedStandym != null) {
                        rp410.setStandym(formattedStandym);
                        isStandymSet = true; // 중복으로 설정되지 않도록 플래그 설정
                    }
                }

                // ASKAMT 값 처리
                BigDecimal askamt = extractAskamt(parsedText);
                if (askamt != null) {
                    rp410.setAskamt(askamt);
                }

                // 사용량 및 사용열량 값을 추출
                List<BigDecimal> usageValues = extractSixUsageValues(text);
                if (usageValues != null && usageValues.size() == 6) {
                    rp410.setSmuseqty(usageValues.get(0));
                    rp410.setSmusehqty(usageValues.get(1));
                    rp410.setLmuseqty(usageValues.get(2));
                    rp410.setLmusehqty(usageValues.get(3));
                    rp410.setLyuseqty(usageValues.get(4));
                    rp410.setLyusehqty(usageValues.get(5));

                }

                // 각 페이지에서 텍스트를 추출하고 amt와 divisor 값을 계산



             /*   // 예시: askamt와 usageValues가 null이 아닌지 확인 후 값을 설정
                if (usageValues != null && usageValues.size() == 6 && askamt != null) {
                    BigDecimal useuamt = askamt.divide(usageValues.get(1), 4, RoundingMode.HALF_UP);
                    rp410.setUseuamt(useuamt);
                } else {
                    // 오류 처리: null 값이 발생했을 경우에 대한 로그 남기기
                    if (usageValues == null || usageValues.size() != 6) {
                        System.err.println("usageValues가 null이거나 크기가 6이 아닙니다.");
                    }
                    if (askamt == null) {
                        System.err.println("askamt가 null입니다.");
                    }
                }*/

                // 가스사용료 값 추출 및 저장
                BigDecimal gasUseAmt = extractGasUseAmt(parsedText);
                if (gasUseAmt != null) {
                    rp410.setGasuseamt(gasUseAmt);
                }

                // 계량기관리비 값 추출 및 저장
                BigDecimal meterMgAmt = extractMeterMgAmt(parsedText);
                if (meterMgAmt != null) {
                    rp410.setMetermgamt(meterMgAmt);
                }

                // 수입부과금환급 값 추출 및 저장
                BigDecimal imtarrAmt = extractImtarrAmt(parsedText);
                if (imtarrAmt != null) {
                    rp410.setImtarramt(imtarrAmt);
                }

                // 안전관리부담금제외 값 추출 및 저장
                BigDecimal safeMgAmt = extractSafeMgAmt(parsedText);
                if (safeMgAmt != null) {
                    rp410.setSafemgamt(safeMgAmt);
                }

                // 공급가액 값 추출 및 저장
                BigDecimal suppAmt = extractSuppAmt(parsedText);
                if (suppAmt != null) {
                    rp410.setSuppamt(suppAmt);
                }

                // 부가세 값 추출 및 저장
                BigDecimal taxAmt = extractTaxAmt(parsedText);
                if (taxAmt != null) {
                    rp410.setTaxamt(taxAmt);
                }

                // 절사액 값 추출 및 저장
                BigDecimal trunAmt = extractTrunAmt(parsedText);
                if (trunAmt != null) {
                    rp410.setTrunamt(trunAmt);
                }

            }

            BigDecimal amt = null;
            BigDecimal divisor = null;
            for (int i = 0; i < extractedTexts.size(); i++) {
                String page = extractedTexts.get(i);
                System.out.println("페이지 " + (i + 1) + ": " + page);  // 페이지 텍스트 출력
                List<BigDecimal> valuesDivide = extractValues(page);

                if (valuesDivide != null && valuesDivide.size() == 3) {
                    if(valuesDivide.get(0) != null){
                        amt = valuesDivide.get(0);

                    }
                    if(valuesDivide.get(2) != null){
                        divisor = valuesDivide.get(2);
                    }

                    // amt와 divisor가 null인지 확인
                    if (amt == null) {
                        System.out.println("페이지 " + (i + 1) + ": amt가 null입니다.");
                    }
                    if (divisor == null) {
                        System.out.println("페이지 " + (i + 1) + ": divisor가 null입니다.");
                    }
                    // 유효한 값이 있을 때만 나눗셈 수행
                } else {
                    System.out.println("페이지 " + (i + 1) + ": 값을 추출할 수 없습니다.");
                }
            }
            if (amt != null && divisor != null && divisor.compareTo(BigDecimal.ZERO) != 0) {
                BigDecimal useuamt = amt.divide(divisor, 4, RoundingMode.HALF_UP);
                rp410.setUseuamt(useuamt);
//                System.out.println("페이지 " + (i + 1) + ": useuamt 설정 완료: " + useuamt);
            } else {
//                System.out.println("페이지 " + (i + 1) + ": 나눗셈을 수행할 수 없습니다.");
            }


//            rp410.setIndatem(now);

                rp410.setSpworkcd(spworkcd);
                rp410.setSpcompcd(spcompcd);
                rp410.setSpplancd(spplancd);
                rp410.setSpplannm(spplannm);
                rp410.setSpworknm(spworknm);
                rp410.setSpcompnm(spcompnm);
                rp410.setInuserid(String.valueOf(user.getId()));
                rp410.setInusernm(user.getUsername());

                String formattedDate = standdt.replace("-", "");
                rp410.setStanddt(formattedDate);

                // 데이터베이스에 엔티티 저장
                rp410 = this.tbRp410Repository.save(rp410);
//                logger.info("데이터베이스에 저장된 엔티티: {}", rp410);

                result.success = true;
                result.message = "PDF 파일 처리 및 데이터 저장 완료";
                result.data = rp410;



            // 5. 임시 파일 삭제
            if (tempFile.delete()) {
//                logger.info("임시 파일 삭제 완료");
            } else {
                logger.warn("임시 파일 삭제 실패, 파일 경로: {}", tempFile.getAbsolutePath());
            }

            return result;

        } catch (IOException e) {
            logger.error("PDF 파일 처리 중 IOException 발생", e);
            result.success = false;
            result.message = "PDF 파일 처리 중 오류가 발생했습니다.";
        } catch (Exception e) {
            logger.error("예상치 못한 오류 발생", e);
            result.success = false;
            result.message = "예상치 못한 오류가 발생했습니다.";
        } finally {
            // 임시 파일이 존재하는 경우 삭제
            if (tempFile != null && tempFile.exists()) {
                try {
                    tempFile.delete();
//                    logger.info("임시 파일 finally 블록에서 삭제 완료");
                } catch (Exception e) {
                    logger.error("finally 블록에서 임시 파일 삭제 중 오류 발생", e);
                }
            }
        }

        return result;
    }


    private String convertToStandymFormat(String text) {
        Pattern yearPattern = Pattern.compile("(\\d{4})년");
        Pattern monthPattern = Pattern.compile("(\\d{2})월");

        Matcher yearMatcher = yearPattern.matcher(text);
        Matcher monthMatcher = monthPattern.matcher(text);

        String year = null;
        String month = null;

        if (yearMatcher.find()) {
            year = yearMatcher.group(1);  // 2023
        }
        if (monthMatcher.find()) {
            month = monthMatcher.group(1); // 05
        }
        // 둘 다 찾았을 때만 반환
        if (year != null && month != null) {
            return year + month;
        }
        return null;
    }


    //  ASKAMT 값을 추출하는 메서드
    private BigDecimal extractAskamt(String text) {
        System.out.println("extractAskamt: " + text);  // 텍스트 출력
        Pattern pattern = Pattern.compile("금액은\\s*(\\d{1,3}(,\\d{3})*)원");
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            String amountString = matcher.group(1);
            System.out.println("추출된 금액 문자열: " + amountString);  // 추출된 문자열 출력
            String cleanedAmountString = amountString.replaceAll(",", "");
            return new BigDecimal(cleanedAmountString);
        }
        return null;
    }



    private List<BigDecimal> extractSixUsageValues(String text) {
        // 마지막 사용열량(MJ) 다음에 나오는 여섯 개의 값을 추출하는 정규식
        Pattern pattern = Pattern.compile("사용열량\\(MJ\\)\\s*(\\d{1,3}(,\\d{3})*(\\.\\d{4})?)\\s*(\\d{1,3}(,\\d{3})*(\\.\\d{4})?)\\s*(\\d{1,3}(,\\d{3})*(\\.\\d{4})?)\\s*(\\d{1,3}(,\\d{3})*(\\.\\d{4})?)\\s*(\\d{1,3}(,\\d{3})*(\\.\\d{4})?)\\s*(\\d{1,3}(,\\d{3})*(\\.\\d{4})?)");
        Matcher matcher = pattern.matcher(text);

        if (matcher.find()) {
            String smuseqtyString = matcher.group(1); // 첫 번째 값 (smuseqty)
            String smusehqtyString = matcher.group(4); // 두 번째 값 (smusehqty)
            String lmuseqtyString = matcher.group(7);
            String lmusehqtyString = matcher.group(10);
            String lyuseqtyString = matcher.group(13);
            String lyusehqtyString = matcher.group(16);

            // 쉼표 제거 후 BigDecimal로 변환
            String cleanedSmuseqtyString = smuseqtyString.replaceAll(",", "");
            String cleanedSmusehqtyString = smusehqtyString.replaceAll(",", "");
            String cleanedLmuseqtyString = lmuseqtyString.replaceAll(",", "");
            String cleanedLmusehqtyString = lmusehqtyString.replaceAll(",", "");
            String cleanedLyuseqtyString = lyuseqtyString.replaceAll(",", "");
            String cleanedLyusehqtyString = lyusehqtyString.replaceAll(",", "");

            BigDecimal smuseqty = new BigDecimal(cleanedSmuseqtyString);
            BigDecimal smusehqty = new BigDecimal(cleanedSmusehqtyString);
            BigDecimal lmuseqty = new BigDecimal(cleanedLmuseqtyString);
            BigDecimal lmusehqty = new BigDecimal(cleanedLmusehqtyString);
            BigDecimal lyuseqty= new BigDecimal(cleanedLyuseqtyString);
            BigDecimal lyusehqty = new BigDecimal(cleanedLyusehqtyString);




            return Arrays.asList(smuseqty, smusehqty,lmuseqty,lmusehqty,lyuseqty,lyusehqty); // 리스트로 반환
        }

        return null;
    }


    // 가스사용료(GASUSEAMT) 값을 추출하는 메서드
    private BigDecimal extractGasUseAmt(String text) {
        Pattern pattern = Pattern.compile("가스사용료\\s*(\\d{1,3}(,\\d{3})*)");
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            String amountString = matcher.group(1).replaceAll(",", "");
            return new BigDecimal(amountString);
        }
        return null;
    }

    // 계량기관리비(METERMGAMT) 값을 추출하는 메서드
    private BigDecimal extractMeterMgAmt(String text) {
        Pattern pattern = Pattern.compile("계량기관리비\\s*(\\d{1,3}(,\\d{3})*)");
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            String amountString = matcher.group(1).replaceAll(",", "");
            return new BigDecimal(amountString);
        }
        return null;
    }

    // 수입부과금환급(IMTARRAMT) 값을 추출하는 메서드
    private BigDecimal extractImtarrAmt(String text) {
        Pattern pattern = Pattern.compile("수입부과금환급\\s*(-?\\d{1,3}(,\\d{3})*)");
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            String amountString = matcher.group(1).replaceAll(",", "");
            return new BigDecimal(amountString);
        }
        return null;
    }

    // 안전관리부담금제외(SAFEMGAMT) 값을 추출하는 메서드
    private BigDecimal extractSafeMgAmt(String text) {
        Pattern pattern = Pattern.compile("안전관리부담금제외\\s*(-?\\d{1,3}(,\\d{3})*)");
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            String amountString = matcher.group(1).replaceAll(",", "");
            return new BigDecimal(amountString);
        }
        return null;
    }

    // 공급가액(SUPPAMT) 값을 추출하는 메서드
    private BigDecimal extractSuppAmt(String text) {
        Pattern pattern = Pattern.compile("공급가액\\s*(\\d{1,3}(,\\d{3})*)");
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            String amountString = matcher.group(1).replaceAll(",", "");
            return new BigDecimal(amountString);
        }
        return null;
    }

    // 부가세(TAXAMT) 값을 추출하는 메서드
    private BigDecimal extractTaxAmt(String text) {
        Pattern pattern = Pattern.compile("부가세\\s*(\\d{1,3}(,\\d{3})*)");
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            String amountString = matcher.group(1).replaceAll(",", "");
            return new BigDecimal(amountString);
        }
        return null;
    }

    // 절사액(TRUNAMT) 값을 추출하는 메서드
    private BigDecimal extractTrunAmt(String text) {
        Pattern pattern = Pattern.compile("절사액\\s*(-?\\d{1,3}(,\\d{3})*)");
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            String amountString = matcher.group(1).replaceAll(",", "");
            return new BigDecimal(amountString);
        }
        return null;
    }

    private List<BigDecimal> extractTwoUsageValues(String text) {
        System.out.println("extractTwoUsageValues: " + text);  // 텍스트 출력
        Pattern pattern = Pattern.compile("사용열량\\(MJ\\)\\s*(\\d{1,3}(,\\d{3})*(\\.\\d{4})?)\\s*(\\d{1,3}(,\\d{3})*(\\.\\d{4})?)");
        Matcher matcher = pattern.matcher(text);

        if (matcher.find()) {
            String smuseqtyString = matcher.group(1);
            String smusehqtyString = matcher.group(4);
            System.out.println("추출된 사용열량: " + smuseqtyString + ", " + smusehqtyString);  // 추출된 값 출력
            String cleanedSmuseqtyString = smuseqtyString.replaceAll(",", "");
            String cleanedSmusehqtyString = smusehqtyString.replaceAll(",", "");

            BigDecimal smuseqty = new BigDecimal(cleanedSmuseqtyString);
            BigDecimal smusehqty = new BigDecimal(cleanedSmusehqtyString);

            return Arrays.asList(smuseqty, smusehqty);
        }
        return null;
    }


    private List<BigDecimal> extractValues(String text) {
        // 금액 추출 패턴
        Pattern amountPattern = Pattern.compile("금액은\\s*(\\d{1,3}(,\\d{3})*)원");
        Matcher amountMatcher = amountPattern.matcher(text);

        BigDecimal askamt = null;
        if (amountMatcher.find()) {
            String amountString = amountMatcher.group(1);
            System.out.println("추출된 금액 문자열: " + amountString);  // 추출된 문자열 출력
            String cleanedAmountString = amountString.replaceAll(",", "");
            askamt = new BigDecimal(cleanedAmountString);
        }

        // 사용열량 추출 패턴
        Pattern usagePattern = Pattern.compile("사용열량\\(MJ\\)\\s*(\\d{1,3}(,\\d{3})*(\\.\\d{4})?)\\s*(\\d{1,3}(,\\d{3})*(\\.\\d{4})?)");
        Matcher usageMatcher = usagePattern.matcher(text);

        BigDecimal smuseqty = null;
        BigDecimal smusehqty = null;
        if (usageMatcher.find()) {
            String smuseqtyString = usageMatcher.group(1);
            String smusehqtyString = usageMatcher.group(4);
            System.out.println("추출된 사용열량: " + smuseqtyString + ", " + smusehqtyString);  // 추출된 값 출력
            String cleanedSmuseqtyString = smuseqtyString.replaceAll(",", "");
            String cleanedSmusehqtyString = smusehqtyString.replaceAll(",", "");

            smuseqty = new BigDecimal(cleanedSmuseqtyString);
            smusehqty = new BigDecimal(cleanedSmusehqtyString);
        }

        // 추출된 값들을 Arrays.asList로 반환
        return Arrays.asList(askamt, smuseqty, smusehqty);
    }


}



