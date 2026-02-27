package dev.controller.servlet.user;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@Builder
public class LoginServlet {
	// 1. 고객 기본/부가 정보 (String 매핑)
    private String basYh;           // 기준시점(분기)
    private String seq;             // 고객번호
    private String age;             // 연령대
    private String sexCd;           // 성별
    private String mbrRk;           // 회원등급
    private String attYm;           // 입회년월
    private String housSidoNm;      // 자택주소시도명
    private String digtChnlRegYn;   // 디지털채널가입여부
    private String digtChnlUseYn;   // 디지털채널이용여부(당월)
    private String lifeStage;       // 라이프스테이지

    // 2. 고객 거래 정보 (DEC 18 -> long 매핑)
    private long totUseAm;          // 총이용금액
    private long crdslUseAm;        // 신용카드이용금액
    private long cnfUseAm;          // 체크카드이용금액

    // 3. 기본업종(대분류) 이용 정보 (long 매핑)
    private long interiorAm;        // 가전/가구/주방용품
    private long insuhosAm;         // 보험/병원
    private long offeduAm;          // 사무통신/서적/학원
    private long trvlecAm;          // 여행/레져/문화
    private long fsbzAm;            // 요식업
    private long svcarcAm;          // 용역/수리/건축자재
    private long distAm;            // 유통
    private long plsanitAm;         // 보건위생
    private long clothgdsAm;        // 의류/신변잡화
    private long autoAm;            // 자동차/연료/정비

    // 4. 기본업종(중분류) 이용 정보 (long 매핑)
    private long funitrAm;          // 가구
    private long applncAm;          // 가전제품
    private long hlthfsAm;          // 건강식품
    private long bldmngAm;          // 건물및시설관리
    private long architAm;          // 건축/자재
    private long opticAm;           // 광학제품
    private long agrictrAm;         // 농업
    private long leisureSAm;        // 레져업소
    private long leisurePAm;        // 레져용품
    private long cultureAm;         // 문화/취미
    private long sanitAm;           // 보건/위생
    private long insuAm;            // 보험
    private long offcomAm;          // 사무/통신기기
    private long bookAm;            // 서적/문구
    private long rprAm;             // 수리서비스
    private long hotelAm;           // 숙박업
    private long goodsAm;           // 신변잡화
    private long trvlAm;            // 여행업
    private long fuelAm;            // 연료판매
    private long svcAm;             // 용역서비스
    private long distbnpAm;         // 유통업비영리
    private long distbpAm;          // 유통업영리
    private long groceryAm;         // 음식료품
    private long hosAm;             // 의료기관
    private long clothAm;           // 의류
    private long restrntAm;         // 일반/휴게음식
    private long automntAm;         // 자동차정비/유지
    private long autoslAm;          // 자동차판매
    private long kitwrAm;           // 주방용품
    private long fabricAm;          // 직물
    private long acdmAm;            // 학원
    private long mbrshopAm;         // 회원제형태업소
}
