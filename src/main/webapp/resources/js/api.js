function getReportMonthsExpense(userNo, date) {
    $.ajax({
        method: "GET",
        url: "payment/months",
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
        url: "payment/paymentDates",
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

function updateFixedCost(payload){
	$.ajax({
        method: "POST",
        url: "fixedcost/add",
        data: { 
			"userNo": payload.userNo,
			"cost": payload.amount,
			"category": payload.category,
			"date": payload.month
		 },
        success: function(result) {
			if(!result){
				alert("등록 중 문제가 발생하였습니다.");
			}else{
				// 새로 고침
				location.href = location.href;
				showToast('✅ 고정지출이 등록되었습니다.');
			}
        },
        error: function(xhr) {
            alert("등록 중 문제가 발생하였습니다: " + xhr.status);
        }
    });
}