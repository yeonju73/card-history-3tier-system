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
			getReportMonthsExpense("", getSelected());
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