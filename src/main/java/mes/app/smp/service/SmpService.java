package mes.app.smp.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import mes.domain.entity.UserCode;
import mes.domain.repository.UserCodeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Service
public class SmpService {
	
	@Value("${smp.api.endpoint}")
	private String apiEndpoint;
	
	@Value("${api.key}")
	private String apiKey;
	
	private final RestTemplate restTemplate;
	
	private final ObjectMapper objectMapper = new ObjectMapper();
	
	@Autowired
	public SmpService(RestTemplate restTemplate) {
		this.restTemplate = restTemplate; // 생성자 주입
	}
	
	@Autowired
	private UserCodeRepository userCodeRepository;
	
	@Scheduled(cron = "0 0 * * * *") // 매시 정각에 실행
	public void updateSmpData() {
		LocalDateTime now = LocalDateTime.now();
		String formattedDate = now.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
		int hour = now.getHour();
		
		String uriString = apiEndpoint + "/getSmpWithForecastDemand?serviceKey=" + apiKey +
				"&pageNo=1&numOfRows=100&dataType=json&date=" + formattedDate;
		
		try {
			URI uri = new URI(uriString);
			String response = restTemplate.getForObject(uri, String.class);
			String smp = parseSmp(response, hour, "육지");  // SMP 값을 String으로 변경
			
			//System.out.println("SMP URI : " + uri);
//			System.out.println("Fetched SMP: " + smp);
			
			// SMP 데이터 업데이트
			Optional<UserCode> optionalSmp = userCodeRepository.findById(158);
			optionalSmp.ifPresent(code -> {
				code.setValue(smp);
				userCodeRepository.save(code);
			});
			
			// 현재 시각 업데이트
			Optional<UserCode> optionalTime = userCodeRepository.findById(159);
			optionalTime.ifPresent(code -> {
				code.setValue(LocalDateTime.now().toString());
				userCodeRepository.save(code);
			});
			
			System.out.println(hour + "시의 SMP 정보가 업데이트 되었습니다. SMP 값: " + smp + ", 업데이트 시간: " + now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
			
		} catch (URISyntaxException e) {
			System.err.println("Error in accessing the API: " + e.getMessage());
		}
	}
	
	private String parseSmp(String response, int targetHour, String targetAreaName) {
		try {
			JsonNode rootNode = objectMapper.readTree(response);
			JsonNode itemsNode = rootNode.path("response").path("body").path("items").path("item");
			
			for (JsonNode item : itemsNode) {
				int hour = item.path("hour").asInt();
				String areaName = item.path("areaName").asText();
				if (hour == targetHour && areaName.equals(targetAreaName)) {
					double smpValue = item.path("smp").asDouble();
					return String.format("%.2f", smpValue);
				}
			}
		} catch (Exception e) {
			System.err.println("Failed to parse JSON response: " + e.getMessage());
		}
		return "데이터없음"; // 유효한 데이터가 없을 경우 "데이터없음"을 반환
	}
	
	
}