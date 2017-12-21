function request_click(){
	var request_method = $("#request_method").val();
	var params =  {};
	debugger;
	$.ajax({
        type:$("#request_method").val() ,     //post
        url:  $("#requst_url").val(),     //url 后台会告诉你
        data:params,
        success:function (data) {   //如果成功获取
        	console.log(data);
        },
        error: function(data){
        	
        }
    })
}