$.ajaxSetup({
    error: function(xhr, textStatus, errorThrown) {
        // 1. 서버가 죽어서 응답이 아예 없는 경우 (status 0)
        if (xhr.status === 0) {
            console.warn("Nginx 1(80)이 응답하지 않습니다. Nginx 2(81)로 자동 재시도합니다.");
            
            // 2. 현재 요청의 URL을 81번 포트 주소로 변경
            // 예: "payment/months" -> "http://localhost:81/sample-project/payment/months"
            this.url = "http://localhost:81/sample-project/" + this.url;
            
            // 3. 세션(쿠키) 유지를 위한 설정 추가
            this.xhrFields = { withCredentials: true };

            // 4. 변경된 주소로 다시 요청 (이게 핵심!)
            return $.ajax(this);
        }
    }
});

function getReportMonthsExpense(userNo, date) {
    $.ajax({
        method: "GET",
        url: "paymentmonths",
        data: { "date": date, "userNo": userNo },
        success: function(data) {
            currentData = transformData(data); 
            renderAll(currentData);
        },
        error: function(xhr) {
            alert("데이터 로드 실패: " + xhr.status);
        }
    });
}

function getPaymentDates(userNo) {
	$.ajax({
        method: "GET",
        url: "paymentDates",
        data: { "userNo": userNo },
        success: function(dates) {
			MONTHS = dates;
			initSelect();
			getReportMonthsExpense("WDJXI9MJ1X41AITHZ3IU", getSelected());
        },
        error: function(xhr) {
            alert("데이터 로드 실패: " + xhr.status);
        }
    });
}

/**
 * 고정지출 등록 페이지 전용 날짜 로드
 */
function getPaymentDatesForFixed(userNo) {
    $.ajax({
        method: "GET",
        url: "paymentDates", // 서블릿 경로
        data: { "userNo": userNo },
        success: function(dates) {
            // fixed.js에 있는 셀렉트 박스 초기화 함수 호출
            initApplyMonthSelect(dates);
        },
        error: function(xhr) {
            alert("날짜 목록 로드 실패: " + xhr.status);
        }
    });
}

/**
 * 고정지출 데이터 서버 전송 (UPDATE 실행)
 */
function updateFixedCost(payload) {
    $.ajax({
        method: "POST",
        url: "addfixedcost",
        data: { 
            "userNo": payload.userNo,
            "cost": payload.amount,
            "category": payload.category, // 예: INSU_AMDEC
            "date": payload.month         // 예: 2023q3
        },
        success: function(result) {
            // 서블릿에서 out.print(isSuccess)로 보낸 boolean 값 처리
            if (result === true || result === "true") {
                showToast('✅ 고정지출이 등록되었습니다.');
                // 1.5초 후 메인 페이지(소비 패턴)로 이동하여 결과 확인
                setTimeout(() => {
                    location.href = "index.html"; 
                }, 1500);
            } else {
                alert("등록 실패: 해당 분기에 데이터가 존재하지 않습니다.");
            }
        },
        error: function(xhr) {
            alert("서버 통신 중 에러가 발생하였습니다: " + xhr.status);
        }
    });
}