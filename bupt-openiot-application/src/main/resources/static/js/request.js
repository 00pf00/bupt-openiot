function request_click(){
	var request_method = $("#request_method").val();
	var params =  $("#request_params").val();
	debugger;
	$.ajax({
        type:$("#request_method").val() ,     //post
        url:  $("#requst_url").val(),     //url 后台会告诉你
        data:JSON.stringify(params),
        success:function (data) {   //如果成功获取
        	console.log(data);
        },
        error: function(data){
        	
        }
    })
}